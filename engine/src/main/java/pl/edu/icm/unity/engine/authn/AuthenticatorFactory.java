/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstanceMetadata;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorTypeDescription;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.store.types.AuthenticatorConfiguration;

/**
 * Instantiation can be done in two variants for local and remote authenticators.
 */
@Component
class AuthenticatorFactory
{
	private AuthenticatorsRegistry authRegistry;
	private IdentityResolver identitiesResolver;
	
	@Autowired
	AuthenticatorFactory(AuthenticatorsRegistry authRegistry, IdentityResolver identitiesResolver)
	{
		this.authRegistry = authRegistry;
		this.identitiesResolver = identitiesResolver;
	}

	AuthenticatorInstance createRemoteAuthenticator(AuthenticatorConfiguration deserialized, String binding)
	{
		CredentialVerificator verificator = createVerificator(deserialized.getVerificationMethod(), 
				deserialized.getName());
		CredentialRetrieval retrieval = createRetrieval(verificator, deserialized.getName(), binding);
		AuthenticatorInstanceMetadata metadata = createMetadata(deserialized.getName(), deserialized.getRevision(), 
				verificator, false);
		AuthenticatorImpl ret = new AuthenticatorImpl(retrieval, verificator, metadata);
		ret.updateConfiguration(deserialized.getConfiguration(), deserialized.getConfiguration(), null);
		return ret;
	}
	
	AuthenticatorInstance createLocalAuthenticator(
			AuthenticatorConfiguration deserialized, String localCredentialConfiguration, String binding)
	{
		CredentialVerificator verificator = createVerificator(deserialized.getVerificationMethod(), 
				deserialized.getName());
		CredentialRetrieval retrieval = createRetrieval(verificator, deserialized.getName(), binding);
		AuthenticatorInstanceMetadata metadata = createMetadata(deserialized.getName(), deserialized.getRevision(), verificator, true);
		AuthenticatorImpl ret = new AuthenticatorImpl(retrieval, verificator, metadata);
		ret.updateConfiguration(localCredentialConfiguration, deserialized.getConfiguration(), deserialized.getLocalCredentialName());
		return ret;
	}
	
	private CredentialVerificator createVerificator(String verificationMethod, String id)
	{
		CredentialVerificatorFactory verificatorFact = authRegistry.getCredentialVerificatorFactory(
				verificationMethod);
		CredentialVerificator verificator = verificatorFact.newInstance();
		verificator.setIdentityResolver(identitiesResolver);
		verificator.setInstanceName(id);
		return verificator;
	}
	
	private CredentialRetrieval createRetrieval(CredentialVerificator verificator, String id, String binding)
	{
		CredentialRetrievalFactory retrievalFact = authRegistry.findCredentialRetrieval(binding, 
				verificator.getExchangeId());
		CredentialRetrieval retrieval = retrievalFact.newInstance();
		retrieval.setCredentialExchange(verificator, id);
		return retrieval;
	}

	private AuthenticatorInstanceMetadata createMetadata(String name, long revision, CredentialVerificator verificator, 
			boolean isLocal)
	{
		AuthenticatorInstanceMetadata instanceMetadata = new AuthenticatorInstanceMetadata();
		instanceMetadata.setRevision(revision);
		instanceMetadata.setId(name);
		AuthenticatorTypeDescription authDesc = getAuthenticatorDescription(verificator, isLocal);
		instanceMetadata.setTypeDescription(authDesc);
		return instanceMetadata;
	}
	
	private AuthenticatorTypeDescription getAuthenticatorDescription(CredentialVerificator verificator, 
			boolean isLocal)
	{
		return new AuthenticatorTypeDescription(
				verificator.getName(), 
				verificator.getDescription(), 
				isLocal);
	}
}
