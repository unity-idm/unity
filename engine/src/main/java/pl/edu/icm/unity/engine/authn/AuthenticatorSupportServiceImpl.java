/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator.VerificatorType;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.engine.endpoint.EndpointsUpdater;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.generic.AuthenticatorConfigurationDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.types.AuthenticatorConfiguration;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;

/**
 * See {@link AuthenticatorSupportService}
 */
@Primary
@Component
public class AuthenticatorSupportServiceImpl implements AuthenticatorSupportService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, AuthenticatorSupportServiceImpl.class);
	private AuthenticatorLoader authnLoader;
	private AuthenticatorsRegistry authnRegistry;
	private AuthenticatorManagement authenticationManagement;
	private AuthenticatorConfigurationDB authenticatorDB;
	private EndpointsUpdater endpointsUpdater;
	private TransactionalRunner tx;
	
	@Autowired
	public AuthenticatorSupportServiceImpl(AuthenticatorLoader authnLoader, 
			AuthenticatorsRegistry authnRegistry, AuthenticatorManagement authenticationManagement,
			AuthenticatorConfigurationDB authenticatorDB,
			EndpointsUpdater endpointsUpdater, TransactionalRunner tx)
	{
		this.authnLoader = authnLoader;
		this.authnRegistry = authnRegistry;
		this.authenticationManagement = authenticationManagement;
		this.authenticatorDB = authenticatorDB;
		this.endpointsUpdater = endpointsUpdater;
		this.tx = tx;
	}


	@Override
	@Transactional
	public List<AuthenticationFlow> getRemoteAuthenticatorsAsFlows(String bindingId) throws EngineException
	{
		ArrayList<AuthenticationFlowDefinition> flows = new ArrayList<>();

		Collection<AuthenticatorInfo> authnInstances = authenticationManagement.getAuthenticators(bindingId);
		for (AuthenticatorInfo authenticator : authnInstances)
		{
			CredentialVerificatorFactory factory = authnRegistry
					.getCredentialVerificatorFactory(authenticator.getTypeDescription().getVerificationMethod());
			CredentialVerificator verificator = factory.newInstance();
			if (verificator.getType().equals(VerificatorType.Remote))
			{
				AuthenticationFlowDefinition authnFlow = new AuthenticationFlowDefinition(authenticator.getId(),
						Policy.NEVER, Sets.newHashSet(authenticator.getId()));
				flows.add(authnFlow);
			}
		}
		return authnLoader.createAuthenticationFlows(flows, bindingId);
	}

	@Override
	@Transactional
	public List<AuthenticationFlow> resolveAuthenticationFlows(List<String> authnOptions, String bindingId)
	{
		return authnLoader.resolveAuthenticationFlows(authnOptions, bindingId);
	}


	@Override
	@Transactional
	public List<AuthenticatorInstance> getRemoteAuthenticators(String bindingId) throws EngineException
	{
		ArrayList<AuthenticatorInstance> ret = new ArrayList<>();

		Collection<AuthenticatorInstance> authnInstances = authnLoader.getAuthenticators(bindingId);
		for (AuthenticatorInstance authenticator : authnInstances)
		{
			if (authenticator.getMetadata().getLocalCredentialName() == null)
			{
				ret.add(authnLoader.getAuthenticator(authenticator.getMetadata().getId(), 
						bindingId));
			}
		}
		return ret;
	}


	@Override
	public void refreshAuthenticatorsOfCredential(String credential) throws EngineException
	{
		tx.runInTransaction(() -> 
		{
			List<AuthenticatorConfiguration> authenticators = authenticatorDB.getAll();
			for (AuthenticatorConfiguration authenticator: authenticators)
			{
				if (credential.equals(authenticator.getLocalCredentialName()))
					refreshAuthenticator(authenticator);
			}
			
		});
		endpointsUpdater.updateManual();
	}
	
	private void refreshAuthenticator(AuthenticatorConfiguration authenticator)
	{
		log.info("Updating authenticator {} as its local credential configuration has changed", 
				authenticator.getName());
		AuthenticatorConfiguration updatedConfiguration = new AuthenticatorConfiguration(
				authenticator.getName(), 
				authenticator.getVerificationMethod(), 
				authenticator.getConfiguration(), 
				authenticator.getLocalCredentialName(), 
				authenticator.getRevision() + 1);
		authenticatorDB.update(updatedConfiguration);
	}
}
