/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.identity.IdentityHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.store.api.generic.CredentialRequirementDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;

/**
 * Credential requirement management implementation.
 * @author K. Benedyczak
 */
@Component
@Primary
@InvocationEventProducer
@Transactional
public class CredentialReqManagementImpl implements CredentialRequirementManagement
{
	private LocalCredentialsRegistry localCredReg;
	private CredentialRepository credRepository;
	private CredentialRequirementDB credentialRequirementDB;
	private CredentialReqRepository credReqRepository;
	private IdentityHelper identityHelper;
	private InternalAuthorizationManager authz;
	private EntityCredentialsHelper entityCredHelper;
	
	@Autowired
	public CredentialReqManagementImpl(LocalCredentialsRegistry localCredReg,
			CredentialRepository credRepository, CredentialRequirementDB credentialRequirementDB,
			IdentityHelper identityHelper, InternalAuthorizationManager authz,
			EntityCredentialsHelper entityCredHelper, CredentialReqRepository credReqRepository)
	{
		this.localCredReg = localCredReg;
		this.credRepository = credRepository;
		this.credentialRequirementDB = credentialRequirementDB;
		this.identityHelper = identityHelper;
		this.authz = authz;
		this.entityCredHelper = entityCredHelper;
		this.credReqRepository = credReqRepository;
	}


	@Override
	public void addCredentialRequirement(CredentialRequirements toAdd) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		assertIsNotSystemCredReq(toAdd.getName());
		assertIsNotReadOnly(toAdd);
		credRepository.assertExist(toAdd.getRequiredCredentials());
		credentialRequirementDB.create(toAdd);
	}

	
	@Override
	public Collection<CredentialRequirements> getCredentialRequirements()
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return credReqRepository.getCredentialRequirements();
	}
	
	@Override
	public void updateCredentialRequirement(CredentialRequirements updated) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		assertIsNotSystemCredReq(updated.getName());
		assertIsNotReadOnly(updated);
		Map<String, CredentialDefinition> credDefs = credRepository
				.getCredentialDefinitions().stream()
				.collect(Collectors.toMap(c -> c.getName(), c -> c));
		CredentialRequirementsHolder.checkCredentials(updated, credDefs, localCredReg);
		credentialRequirementDB.update(updated);
	}

	@Override
	public void removeCredentialRequirement(String toRemove, String replacementId) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		assertIsNotSystemCredReq(toRemove);
		Set<Long> entities = identityHelper.getEntitiesByRootAttribute(
				CredentialAttributeTypeProvider.CREDENTIAL_REQUIREMENTS,
				Collections.singleton(toRemove));
		if (entities.size() > 0 && replacementId == null)
			throw new IllegalCredentialException("There are entities with the removed credential requirements set and a replacement was not specified.");
		if (replacementId != null)
		{
			for (Long entityId: entities)
				entityCredHelper.setEntityCredentialRequirementsNoCheck(entityId, replacementId);
		}

		credentialRequirementDB.delete(toRemove);
	}
	
	private void assertIsNotSystemCredReq(String name)
	{
		if (SystemAllCredentialRequirements.NAME.equals(name))
			throw new IllegalArgumentException("Credential requirement '" + name + "' is the system credential requirement and cannot be overwrite or remove");
	}
	
	private void assertIsNotReadOnly(CredentialRequirements cred) throws EngineException
	{
		if (cred.isReadOnly())
			throw new IllegalArgumentException("Cannot create read only credential requirement through this API");
	}
}
