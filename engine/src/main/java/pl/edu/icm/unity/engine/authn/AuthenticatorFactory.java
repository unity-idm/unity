/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.store.types.AuthenticatorConfiguration;
import pl.edu.icm.unity.types.authn.AuthenticatorInstanceMetadata;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;

/**
 * Instantiation can be done in two scenarios, each in two variants:
 * <ul>
 * <li>either a constructor is called with a state loaded previously from persisted storage and provided in    
 * {@link AuthenticatorFactory#setAuthenticatorInstance(AuthenticatorInstanceMetadata)}. If the authenticator is local, 
 * then the local credential must be separately given.
 * <li> Otherwise a full constructor is called to initialize the object completely. In case of a local authenticator
 * a local credential name and its configuration must be provided.
 * </ul>
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

	/**
	 * For initial object creation in case of local authenticator.
	 */
	AuthenticatorInstance createNewLocalAuthenticator(String name, String typeId, String localCredentialName, 
			String localCredentialconfiguration, String binding) throws WrongArgumentException
	{
		CredentialVerificator verificator = createVerificator(typeId, name);
		CredentialRetrieval retrieval = createRetrieval(verificator, name, binding);
		AuthenticatorInstanceMetadata metadata = createMetadata(name, 0, verificator, true);
		AuthenticatorImpl ret = new AuthenticatorImpl(retrieval, verificator, metadata);
		ret.updateConfiguration(localCredentialconfiguration, localCredentialName);
		return ret;
	}
	
	/**
	 * For initial object creation in case of remote authenticator.
	 */
	AuthenticatorInstance createNewRemoteAuthenticator(String name, String typeId, String configuration, String binding)
					throws WrongArgumentException
	{
		CredentialVerificator verificator = createVerificator(typeId, name);
		CredentialRetrieval retrieval = createRetrieval(verificator, name, binding);
		AuthenticatorInstanceMetadata metadata = createMetadata(name, 0, verificator, false);
		AuthenticatorImpl ret = new AuthenticatorImpl(retrieval, verificator, metadata);
		ret.updateConfiguration(configuration, null);
		return ret;
	}	
	
	/**
	 * Used when the state is initialized from persisted storage, for the remote authenticators.
	 */
	AuthenticatorInstance restoreRemoteAuthenticator(AuthenticatorConfiguration deserialized, String binding)
	{
		CredentialVerificator verificator = createVerificator(deserialized.getVerificationMethod(), 
				deserialized.getName());
		CredentialRetrieval retrieval = createRetrieval(verificator, deserialized.getName(), binding);
		AuthenticatorInstanceMetadata metadata = createMetadata(deserialized.getName(), deserialized.getRevision(), 
				verificator, false);
		AuthenticatorImpl ret = new AuthenticatorImpl(retrieval, verificator, metadata);
		ret.updateConfiguration(deserialized.getConfiguration(), null);
		return ret;
	}
	
	/**
	 * Used when the state is initialized from persisted storage, for the local authenticators.
	 */
	AuthenticatorInstance restoreLocalAuthenticator(
			AuthenticatorConfiguration deserialized, String localCredentialConfiguration, String binding)
	{
		CredentialVerificator verificator = createVerificator(deserialized.getVerificationMethod(), 
				deserialized.getName());
		CredentialRetrieval retrieval = createRetrieval(verificator, deserialized.getName(), binding);
		AuthenticatorInstanceMetadata metadata = createMetadata(deserialized.getName(), deserialized.getRevision(), verificator, true);
		AuthenticatorImpl ret = new AuthenticatorImpl(retrieval, verificator, metadata);
		ret.updateConfiguration(localCredentialConfiguration, deserialized.getLocalCredentialName());
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
