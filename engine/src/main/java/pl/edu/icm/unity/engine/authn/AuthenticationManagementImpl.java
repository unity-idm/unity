/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorsRegistry;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.credential.CredentialHolder;
import pl.edu.icm.unity.engine.endpoint.EndpointsUpdater;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.generic.AuthenticatorInstanceDB;
import pl.edu.icm.unity.store.api.generic.CredentialDB;
import pl.edu.icm.unity.store.api.generic.CredentialRequirementDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;
import pl.edu.icm.unity.types.authn.CredentialDefinition;

/**
 * Authentication management implementation.
 * @author K. Benedyczak
 */
@Component
@Primary
@InvocationEventProducer
public class AuthenticationManagementImpl implements AuthenticatorManagement
{
	private AuthenticatorsRegistry authReg;
	private LocalCredentialsRegistry localCredReg;
	private AuthenticatorInstanceDB authenticatorDB;
	private CredentialDB credentialDB;
	private IdentityResolver identityResolver;
	private EndpointsUpdater endpointsUpdater;
	private AuthenticatorLoader authenticatorLoader;
	private AuthorizationManager authz;
	private TransactionalRunner tx;
	
	@Autowired
	public AuthenticationManagementImpl(AuthenticatorsRegistry authReg, TransactionalRunner tx,
			AuthenticatorInstanceDB authenticatorDB,
			CredentialDB credentialDB, CredentialRequirementDB credentialRequirementDB,
			IdentityResolver identityResolver, 
			EndpointsUpdater endpointsUpdater, AuthenticatorLoader authenticatorLoader,
			AttributeTypeDAO dbAttributes, AuthorizationManager authz, 
			LocalCredentialsRegistry localCredReg, UnityMessageSource msg)
	{
		this.authReg = authReg;
		this.tx = tx;
		this.localCredReg = localCredReg;
		this.authenticatorDB = authenticatorDB;
		this.credentialDB = credentialDB;
		this.identityResolver = identityResolver;
		this.endpointsUpdater = endpointsUpdater;
		this.authenticatorLoader = authenticatorLoader;
		this.authz = authz;
	}



	@Override
	public Collection<AuthenticatorTypeDescription> getAuthenticatorTypes(String bindingId)
			throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		if (bindingId == null)
			return authReg.getAuthenticators();
		return authReg.getAuthenticatorsByBinding(bindingId);
	}

	@Override
	@Transactional
	public AuthenticatorInstance createAuthenticator(String id, String typeId, String jsonVerificatorConfig,
			String jsonRetrievalConfig, String credentialName) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		AuthenticatorImpl authenticator;
		if (credentialName != null)
		{
			CredentialDefinition credentialDef = credentialDB.get(credentialName);
			CredentialHolder credential = new CredentialHolder(credentialDef, localCredReg);
			String credentialConfiguration = credential.getCredentialDefinition().getConfiguration();
			authenticator = new AuthenticatorImpl(identityResolver, authReg, id, typeId, 
					jsonRetrievalConfig, credentialName, credentialConfiguration);

			verifyIfLocalCredentialMatchesVerificator(authenticator, credential, 
					credentialName);
		} else
		{
			authenticator = new AuthenticatorImpl(identityResolver, authReg, id, typeId, 
					jsonRetrievalConfig, jsonVerificatorConfig);
		}
		authenticatorDB.create(authenticator.getAuthenticatorInstance());
		return authenticator.getAuthenticatorInstance();
	}

	@Override
	public Collection<AuthenticatorInstance> getAuthenticators(String bindingId)
			throws EngineException
	{
		List<AuthenticatorInstance> ret = tx.runInTransactionRetThrowing(() -> {
			authz.checkAuthorization(AuthzCapability.maintenance);
			return authenticatorDB.getAll();
		});
		
		if (bindingId != null)
		{
			for (Iterator<AuthenticatorInstance> iter = ret.iterator(); iter.hasNext();)
			{
				AuthenticatorInstance authnInstance = iter.next();
				if (!bindingId.equals(authnInstance.getTypeDescription().getSupportedBinding()))
				{
					iter.remove();
				}
			}
		}
		return ret;
	}

	@Override
	public void updateAuthenticator(String id, String verificatorConfig,
			String jsonRetrievalConfig, String localCredential) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		
		tx.runInTransactionThrowing(() -> {
			AuthenticatorImpl current = authenticatorLoader.getAuthenticator(id);
			String verificatorConfigCopy = verificatorConfig;
			if (localCredential != null)
			{
				CredentialDefinition credentialDef = credentialDB.get(localCredential);
				CredentialHolder credential = new CredentialHolder(credentialDef, localCredReg);
				verificatorConfigCopy = credential.getCredentialDefinition().getConfiguration();
				verifyIfLocalCredentialMatchesVerificator(current, credential, 
						localCredential);
			}
			
			current.updateConfiguration(jsonRetrievalConfig, verificatorConfigCopy, localCredential);
			authenticatorDB.update(current.getAuthenticatorInstance());
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
	
	private void verifyIfLocalCredentialMatchesVerificator(AuthenticatorImpl authenticator,
			CredentialHolder credential, String requestedLocalCredential) throws IllegalCredentialException
	{
		String verificationMethod = authenticator.getAuthenticatorInstance().
				getTypeDescription().getVerificationMethod();
		if (!credential.getCredentialDefinition().getTypeId().equals(verificationMethod))
			throw new IllegalCredentialException("The local credential " + requestedLocalCredential + 
					"is of different type then the credential suported by the " +
					"authenticator, which is " + verificationMethod);
	}

}
