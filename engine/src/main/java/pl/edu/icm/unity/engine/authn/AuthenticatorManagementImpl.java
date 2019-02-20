/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.credential.CredentialHolder;
import pl.edu.icm.unity.engine.credential.CredentialRepository;
import pl.edu.icm.unity.engine.endpoint.EndpointsUpdater;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.store.api.generic.AuthenticationFlowDB;
import pl.edu.icm.unity.store.api.generic.AuthenticatorConfigurationDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.types.AuthenticatorConfiguration;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;
import pl.edu.icm.unity.types.authn.CredentialDefinition;

/**
 * Authentication management implementation.
 * @author K. Benedyczak
 */
@Component
@Primary
@InvocationEventProducer
public class AuthenticatorManagementImpl implements AuthenticatorManagement
{
	private AuthenticatorsRegistry authReg;
	private LocalCredentialsRegistry localCredReg;
	private AuthenticatorConfigurationDB authenticatorDB;
	private AuthenticationFlowDB authenticationFlowDB;
	private CredentialRepository credentialRepository;
	private EndpointsUpdater endpointsUpdater;
	private AuthenticatorLoader authenticatorLoader;
	private InternalAuthorizationManager authz;
	private TransactionalRunner tx;
	
	@Autowired
	public AuthenticatorManagementImpl(AuthenticatorsRegistry authReg, 
			TransactionalRunner tx,
			AuthenticatorConfigurationDB authenticatorDB,
			AuthenticationFlowDB authenticationFlowDB,
			CredentialRepository credentialRepository,
			EndpointsUpdater endpointsUpdater, 
			AuthenticatorLoader authenticatorLoader,
			InternalAuthorizationManager authz, 
			LocalCredentialsRegistry localCredReg)
	{
		this.authReg = authReg;
		this.tx = tx;
		this.localCredReg = localCredReg;
		this.authenticatorDB = authenticatorDB;
		this.authenticationFlowDB = authenticationFlowDB;
		this.credentialRepository = credentialRepository;
		this.endpointsUpdater = endpointsUpdater;
		this.authenticatorLoader = authenticatorLoader;
		this.authz = authz;
	}

	@Override
	@Transactional
	public AuthenticatorInfo createAuthenticator(String id, String typeId, String configuration,
			String credentialName) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		
		if (authenticationFlowDB.getAllAsMap().get(id) != null)
		{
			throw new IllegalArgumentException(
					"Can not add authenticator " + id
							+ ", authentication flow with the same name exists");
		}
		verifyConfiguration(typeId, configuration, credentialName);
		
		AuthenticatorConfiguration persistedAuthenticator = new AuthenticatorConfiguration(
				id, typeId, configuration, credentialName, 0);
		authenticatorDB.create(persistedAuthenticator);
		return getExposedAuthenticatorInfo(persistedAuthenticator);
	}

	@Override
	public Collection<AuthenticatorInfo> getAuthenticators(String bindingId)
			throws EngineException
	{
		List<AuthenticatorConfiguration> persisted = tx.runInTransactionRetThrowing(() -> 
		{
			authz.checkAuthorization(AuthzCapability.maintenance);
			return authenticatorDB.getAll();
		});
		
		return persisted.stream()
				.map(persistedA -> getExposedAuthenticatorInfo(persistedA))
				.filter(authnInfo -> (bindingId == null || authnInfo.getSupportedBindings().contains(bindingId)))
				.collect(Collectors.toList());
	}

	@Override
	public void updateAuthenticator(String id, String config, String localCredential) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		
		tx.runInTransactionThrowing(() -> 
		{
			AuthenticatorConfiguration currentConfiguration = authenticatorDB.get(id);
			verifyConfiguration(currentConfiguration.getVerificationMethod(), config, localCredential);
			
			AuthenticatorConfiguration updatedConfiguration = new AuthenticatorConfiguration(
					currentConfiguration.getName(), 
					currentConfiguration.getVerificationMethod(), 
					config, 
					localCredential, 
					currentConfiguration.getRevision() + 1);
			authenticatorDB.update(updatedConfiguration);
		});
		endpointsUpdater.updateManual();
	}

	@Override
	@Transactional
	public void removeAuthenticator(String id) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		authenticatorDB.delete(id);
	}
	
	private AuthenticatorInfo getExposedAuthenticatorInfo(AuthenticatorConfiguration persistedAuthenticator)
	{
		return new AuthenticatorInfo(persistedAuthenticator.getName(), 
				authReg.getAuthenticatorTypeById(persistedAuthenticator.getVerificationMethod()), 
				persistedAuthenticator.getConfiguration(), 
				Optional.ofNullable(persistedAuthenticator.getLocalCredentialName()), 
				authReg.getSupportedBindings(persistedAuthenticator.getVerificationMethod()));
	}
	
	private void verifyConfiguration(String typeId, String config, String localCredential) throws IllegalCredentialException
	{
		AuthenticatorTypeDescription typeDescription = authReg.getAuthenticatorTypeById(typeId);
		if (typeDescription == null)
			throw new IllegalArgumentException("Can not add authenticator of unknown type " + typeId);
		String effectiveConfig = config;
		if (localCredential != null)
		{
			CredentialDefinition credentialDef = credentialRepository.get(localCredential);
			CredentialHolder credential = new CredentialHolder(credentialDef, localCredReg);
			effectiveConfig = credential.getCredentialDefinition().getConfiguration();
			verifyIfLocalCredentialMatchesVerificator(typeDescription, credentialDef, localCredential);
		}
		
		authenticatorLoader.verifyConfiguration(typeId, effectiveConfig);
	}
	
	private void verifyIfLocalCredentialMatchesVerificator(AuthenticatorTypeDescription authenticator,
			CredentialDefinition credentialDef, String requestedLocalCredential) throws IllegalCredentialException
	{
		String verificationMethod = authenticator.getVerificationMethod();
		if (!credentialDef.getTypeId().equals(verificationMethod))
			throw new IllegalCredentialException("The local credential " + requestedLocalCredential + 
					"is of different type then the credential suported by the " +
					"authenticator, which is " + verificationMethod);
	}

}
