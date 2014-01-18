/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.authn.CredentialRetrievalFactory;
import pl.edu.icm.unity.server.authn.CredentialVerificator;
import pl.edu.icm.unity.server.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.server.authn.IdentityResolver;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificator;
import pl.edu.icm.unity.server.registries.AuthenticatorsRegistry;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;

/**
 * Internal representation of an authenticator, which is a composition of {@link CredentialRetrieval},
 * {@link CredentialVerificator}, configured.
 * <p>
 * Instantiation is quite complex:
 * either a one arg constructor is called, then state initialized with 
 * {@link AuthenticatorImpl#setAuthenticatorInstance(AuthenticatorInstance)}.
 * Otherwise a multiarg constructor is called to initialize the object completely and it may be followed by a 
 * {@link #setCredentialName(String)} to set a local credential name if the authenticator is local
 * 
 * @author K. Benedyczak
 */
public class AuthenticatorImpl
{
	private CredentialRetrieval retrieval;
	private CredentialVerificator verificator;
	private AuthenticatorsRegistry authRegistry;
	private AuthenticatorInstance instanceDescription;
	private IdentityResolver identitiesResolver;
	
	/**
	 * For initial object creation. Verificator configuration is only required for remote verificators.
	 * @param reg
	 * @param typeId
	 * @param configuration
	 * @throws WrongArgumentException 
	 */
	public AuthenticatorImpl(IdentityResolver identitiesResolver, AuthenticatorsRegistry reg, 
			String name, String typeId, String rConfiguration, String vConfiguration)
					throws WrongArgumentException
	{
		this(identitiesResolver, reg, name);
		AuthenticatorTypeDescription authDesc = authRegistry.getAuthenticatorsById(typeId);
		if (authDesc == null)
			throw new WrongArgumentException("The authenticator type " + typeId + " is not known");
		createCoworkers(authDesc, rConfiguration, vConfiguration);
	}
	
	/**
	 * For cases when object state should be initialized from serialized form
	 * @param reg
	 */
	public AuthenticatorImpl(IdentityResolver identitiesResolver, AuthenticatorsRegistry reg, String name)
	{
		this.authRegistry = reg;
		this.instanceDescription = new AuthenticatorInstance();
		this.instanceDescription.setId(name);
		this.identitiesResolver = identitiesResolver;
	}
	
	private void createCoworkers(AuthenticatorTypeDescription authDesc, String rConfiguration, String vConfiguration)
	{
		CredentialRetrievalFactory retrievalFact = authRegistry.getCredentialRetrievalFactory(
				authDesc.getRetrievalMethod());
		CredentialVerificatorFactory verificatorFact = authRegistry.getCredentialVerificatorFactory(
				authDesc.getVerificationMethod());
		verificator = verificatorFact.newInstance();
		verificator.setIdentityResolver(identitiesResolver);
		if (vConfiguration != null)
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
		instanceDescription.setRetrievalJsonConfiguration(rConfiguration);
		if(vConfiguration!=null)
		setVerificatorConfiguration(vConfiguration);
	}
	
	public void setCredentialName(String credential)
	{
		if (verificator instanceof LocalCredentialVerificator)
		{
			((LocalCredentialVerificator)verificator).setCredentialName(credential);
			instanceDescription.setLocalCredentialName(credential);
		}
	}
	
	/**
	 * Local verificators has configuration provided by a credential definition, the 
	 * configuration for the authenticator is ignored. It must be set via this method
	 * @param configuration
	 */
	public void setVerificatorConfiguration(String vConfiguration)
	{
		verificator.setSerializedConfiguration(vConfiguration);
		instanceDescription.setVerificatorJsonConfiguration(vConfiguration);
	}
	
	/**
	 * Sets {@link AuthenticatorInstance} loaded from persisted storage
	 * @param deserialized
	 */
	public void setAuthenticatorInstance(AuthenticatorInstance deserialized)
	{
		createCoworkers(deserialized.getTypeDescription(), deserialized.getRetrievalJsonConfiguration(),
				deserialized.getVerificatorJsonConfiguration());
		setCredentialName(deserialized.getLocalCredentialName());
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
