/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.IllegalArgumentException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.authn.CredentialRetrievalFactory;
import pl.edu.icm.unity.server.authn.CredentialVerificator;
import pl.edu.icm.unity.server.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.server.authn.IdentityResolver;
import pl.edu.icm.unity.server.registries.AuthenticatorsRegistry;
import pl.edu.icm.unity.types.JsonSerializable;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;

/**
 * Internal representation of an authenticator, which is a composition of {@link CredentialRetrieval},
 * {@link CredentialVerificator}, configured.
 * <p>
 * Instantiation is quite complex:
 * either a one arg constructor is called, then state initialized with 
 * {@link AuthenticatorImpl#setSerializedConfiguration(String)}.
 * Otherwise a multiarg constructor is called to initialize the object completely.
 * @author K. Benedyczak
 */
public class AuthenticatorImpl implements JsonSerializable
{
	private CredentialRetrieval retrieval;
	private CredentialVerificator verificator;
	private AuthenticatorsRegistry authRegistry;
	private AuthenticatorInstance instanceDescription;
	private IdentityResolver identitiesResolver;
	
	/**
	 * For initial object creation
	 * @param reg
	 * @param typeId
	 * @param configuration
	 */
	public AuthenticatorImpl(IdentityResolver identitiesResolver, AuthenticatorsRegistry reg, 
			String name, String typeId, String rConfiguration, String vConfiguration)
	{
		this(identitiesResolver, reg);
		instanceDescription.setId(name);
		AuthenticatorTypeDescription authDesc = authRegistry.getAuthenticatorsById(typeId);
		if (authDesc == null)
			throw new IllegalArgumentException("The authenticator type " + typeId + " is not known");
		createCoworkers(authDesc, rConfiguration, vConfiguration);
	}
	
	/**
	 * For cases when object state should be initialized from serialized form
	 * @param reg
	 */
	public AuthenticatorImpl(IdentityResolver identitiesResolver, AuthenticatorsRegistry reg)
	{
		this.authRegistry = reg;
		this.instanceDescription = new AuthenticatorInstance();
		this.identitiesResolver = identitiesResolver;
	}
	
	private void createCoworkers(AuthenticatorTypeDescription authDesc, String rConfiguration, String vConfiguration)
	{
		CredentialRetrievalFactory retrievalFact = authRegistry.getCredentialRetrievalFactory(
				authDesc.getRetrievalMethod());
		CredentialVerificatorFactory verificatorFact = authRegistry.getCredentialVerificatorFactory(
				authDesc.getVerificationMethod());
		verificator = verificatorFact.newInstance();
		verificator.setSerializedConfiguration(vConfiguration);
		retrieval = retrievalFact.newInstance();
		retrieval.setSerializedConfiguration(rConfiguration);
		retrieval.setCredentialExchange(verificator);
		
		instanceDescription.setRetrievalJsonConfiguration(rConfiguration);
		instanceDescription.setVerificatorJsonConfiguration(vConfiguration);
		instanceDescription.setTypeDescription(authDesc);
	}
	
	public void setConfiguration(String rConfiguration, String vConfiguration)
	{
		retrieval.setSerializedConfiguration(rConfiguration);
		verificator.setSerializedConfiguration(vConfiguration);
		verificator.setIdentityResolver(identitiesResolver);
		instanceDescription.setRetrievalJsonConfiguration(rConfiguration);
		instanceDescription.setVerificatorJsonConfiguration(vConfiguration);
	}
	
	@Override
	public String getSerializedConfiguration()
	{
		try
		{
			return Constants.MAPPER.writeValueAsString(instanceDescription);
		} catch (JsonProcessingException e)
		{
			throw new RuntimeEngineException("Can't serialize authenticator state to JSON", e);
		}
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
		AuthenticatorInstance deserialized;
		try
		{
			deserialized = Constants.MAPPER.readValue(json, AuthenticatorInstance.class);
		} catch (Exception e)
		{
			throw new RuntimeEngineException("Can't deserialize authenticator state from JSON", e);
		}
		
		createCoworkers(deserialized.getTypeDescription(), deserialized.getRetrievalJsonConfiguration(),
				deserialized.getVerificatorJsonConfiguration());
	}

	public AuthenticatorInstance getAuthenticatorInstance()
	{
		return instanceDescription;
	}

	public CredentialRetrieval getRetrieval()
	{
		return retrieval;
	}
}
