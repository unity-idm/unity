/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import static pl.edu.icm.unity.engine.credential.CredentialAttributeTypeProvider.CREDENTIAL_PREFIX;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.identity.IdentityHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.generic.CredentialDB;
import pl.edu.icm.unity.store.api.generic.CredentialRequirementDB;
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
	private CredentialRequirementDB credentialRequirementDB;
	private IdentityHelper identityHelper;
	private AttributeTypeDAO attributeTypeDAO;
	private AttributeDAO attributeDAO;
	private AuthorizationManager authz;
	private UnityMessageSource msg;
	

	@Autowired
	public CredentialManagementImpl(LocalCredentialsRegistry localCredReg,
			CredentialDB credentialDB, CredentialRequirementDB credentialRequirementDB,
			IdentityHelper identityHelper, AttributeTypeDAO attributeTypeDAO,
			AuthorizationManager authz, UnityMessageSource msg)
	{
		this.localCredReg = localCredReg;
		this.credentialDB = credentialDB;
		this.credentialRequirementDB = credentialRequirementDB;
		this.identityHelper = identityHelper;
		this.attributeTypeDAO = attributeTypeDAO;
		this.authz = authz;
		this.msg = msg;
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
		CredentialHolder helper = new CredentialHolder(credentialDefinition, localCredReg);
		credentialDB.create(credentialDefinition);
		AttributeType at = getCredentialAT(helper.getCredentialDefinition().getName());
		attributeTypeDAO.create(at);
	}

	@Override
	public void updateCredentialDefinition(CredentialDefinition updated,
			LocalCredentialState desiredAuthnState) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);

		CredentialHolder helper = new CredentialHolder(updated, localCredReg);
		//get all cred reqs with it
		Set<String> affectedCr = new HashSet<String>();
		List<CredentialRequirements> crs = credentialRequirementDB.getAll();
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
		credentialDB.delete(toRemove);
		attributeTypeDAO.delete(CREDENTIAL_PREFIX+toRemove);
	}


	@Override
	public Collection<CredentialDefinition> getCredentialDefinitions() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return credentialDB.getAll();
	}
	

	private AttributeType getCredentialAT(String name)
	{
		AttributeType credentialAt = new AttributeType(CREDENTIAL_PREFIX+name, 
				StringAttributeSyntax.ID, msg, CREDENTIAL_PREFIX,
				new Object[] {name});
		credentialAt.setMaxElements(1);
		credentialAt.setMinElements(1);
		credentialAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG | AttributeType.INSTANCES_IMMUTABLE_FLAG);
		return credentialAt;
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
}
