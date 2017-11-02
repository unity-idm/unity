/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import static pl.edu.icm.unity.engine.credential.CredentialAttributeTypeProvider.CREDENTIAL_PREFIX;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.identity.IdentityHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.generic.CredentialDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.CredentialType;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Credential management implementation.
 * @author K. Benedyczak
 */
@Component
@Primary
@InvocationEventProducer
@Transactional
public class CredentialManagementImpl implements CredentialManagement
{
	private LocalCredentialsRegistry localCredReg;
	private CredentialDB credentialDB;
	private CredentialReqRepository credentialRequirementRepository;
	private CredentialRepository credentialRepository;
	private IdentityHelper identityHelper;
	private AttributeTypeDAO attributeTypeDAO;
	private AttributeDAO attributeDAO;
	private AuthorizationManager authz;
	private SystemCredentialProvider sysProvider;
	private AttributeTypeHelper attrTypeHelper;
	
	@Autowired
	public CredentialManagementImpl(LocalCredentialsRegistry localCredReg,
			CredentialDB credentialDB, CredentialReqRepository credentialRequirementRepository,
			IdentityHelper identityHelper, AttributeTypeDAO attributeTypeDAO,
			AttributeDAO attributeDAO, AuthorizationManager authz,
			SystemCredentialProvider sysProvider, CredentialRepository credentialRepository, 
			AttributeTypeHelper attrTypeHelper)
	{
		this.localCredReg = localCredReg;
		this.credentialDB = credentialDB;
		this.credentialRequirementRepository = credentialRequirementRepository;
		this.identityHelper = identityHelper;
		this.attributeTypeDAO = attributeTypeDAO;
		this.attributeDAO = attributeDAO;
		this.authz = authz;
		this.sysProvider = sysProvider;
		this.credentialRepository = credentialRepository;
		this.attrTypeHelper = attrTypeHelper;
	}

	@Override
	public Collection<CredentialType> getCredentialTypes() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return localCredReg.getLocalCredentialTypes();
	}

	@Override
	public void addCredentialDefinition(CredentialDefinition credentialDefinition)
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		assertIsNotReadOnly(credentialDefinition);
		assertIsNotSystemProfile(credentialDefinition.getName());
		CredentialHolder helper = new CredentialHolder(credentialDefinition, localCredReg);
		credentialDB.create(credentialDefinition);
		AttributeType at = attrTypeHelper.getCredentialAT(helper.getCredentialDefinition().getName());
		attributeTypeDAO.create(at);
	}

	@Override
	public void updateCredentialDefinition(CredentialDefinition updated,
			LocalCredentialState desiredAuthnState) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		assertIsNotReadOnly(updated);
		assertIsNotSystemProfile(updated.getName());
		
		CredentialHolder helper = new CredentialHolder(updated, localCredReg);
		//get all cred reqs with it
		Set<String> affectedCr = new HashSet<String>();
		Collection<CredentialRequirements> crs = credentialRequirementRepository.getCredentialRequirements();
		for (CredentialRequirements cr: crs)
		{
			if (cr.getRequiredCredentials().contains(updated.getName()))
				affectedCr.add(cr.getName());
		}

		//get all entities with any of the affected CRs
		Set<Long> entities = identityHelper.getEntitiesByRootAttribute(
				CredentialAttributeTypeProvider.CREDENTIAL_REQUIREMENTS, affectedCr);

		//check if the entities credential status will be fine
		for (Long entityId: entities)
			checkEntityCredentialState(entityId, desiredAuthnState, helper);

		credentialDB.update(updated);
	}


	@Override
	public void removeCredentialDefinition(String toRemove) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		assertIsNotSystemProfile(toRemove);
		credentialDB.delete(toRemove);
		attributeTypeDAO.delete(CREDENTIAL_PREFIX+toRemove);
	}

	@Override
	public Collection<CredentialDefinition> getCredentialDefinitions() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return credentialRepository.getCredentialDefinitions();
	}
	
	/**
	 * @param desiredCredState If value is 'correct', then method checks if there is an existing credential and 
	 * if it is correct with the given CredentialHolder. If it is set and incorrect, an exception is thrown. 
	 * If the value is 'outdated' then nothing is done.
	 * If the value is 'notSet' then the credential is removed if the entity has it set. 
	 * @param entityId
	 * @param credentialChanged
	 * @throws EngineException 
	 */
	private void checkEntityCredentialState(long entityId, LocalCredentialState desiredCredState,
			CredentialHolder credentialChanged) throws EngineException
	{
		if (desiredCredState == LocalCredentialState.outdated)
			return;
		String credAttribute = CREDENTIAL_PREFIX + credentialChanged.getCredentialDefinition().getName(); 
		Collection<AttributeExt> attributes = attributeDAO.getEntityAttributes(entityId, credAttribute, "/");
		if (attributes.isEmpty())
			return;
		if (desiredCredState == LocalCredentialState.notSet)
		{
			attributeDAO.deleteAttribute(credAttribute, entityId, "/");
			return;
		}
		String credential = (String)attributes.iterator().next().getValues().get(0);
		CredentialPublicInformation currentState = 
				credentialChanged.getHandler().checkCredentialState(credential);
		if (currentState.getState() != LocalCredentialState.correct && 
				desiredCredState == LocalCredentialState.correct)
			throw new IllegalCredentialException("The new credential is not compatible with "
					+ "the previous definition and can not keep the credential state as correct");
	}
	
	private void assertIsNotSystemProfile(String name)
	{
		Set<String> systemProfiles = sysProvider.getSystemCredentials().stream().map(c -> c.getName()).collect(Collectors.toSet());
		if (systemProfiles.contains(name))
			throw new IllegalArgumentException("Credential '" + name + "' is the system credential and cannot be overwrite or remove");
	}
	
	private void assertIsNotReadOnly(CredentialDefinition cred) throws EngineException
	{
		if (cred.isReadOnly())
			throw new IllegalArgumentException("Cannot create read only credentials through this API");
	}

}
