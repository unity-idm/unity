/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import pl.edu.icm.unity.engine.api.authn.Authenticator;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorsRegistry;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificator;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;

/**
 * Instantiation can be done in two scenarios, each in two variants:
 * <ul>
 * <li>either a constructor is called with a state loaded previously from persisted storage and provided in    
 * {@link AuthenticatorImpl#setAuthenticatorInstance(AuthenticatorInstance)}. If the authenticator is local, 
 * then the local credential must be separately given.
 * <li> Otherwise a full constructor is called to initialize the object completely. In case of a local authenticator
 * a local credential name and its configuration must be provided.
 * </ul>
 * @author P.Piernik
 *
 */
public class AuthenticatorImpl implements Authenticator
{
	private CredentialRetrieval retrieval;
	private CredentialVerificator verificator;
	private AuthenticatorsRegistry authRegistry;
	private AuthenticatorInstance instanceDescription;
	private IdentityResolver identitiesResolver;
	
	/**
	 * For initial object creation in case of local authenticator.
	 * @param reg
	 * @param typeId
	 * @param configuration
	 * @throws WrongArgumentException 
	 */
	public AuthenticatorImpl(IdentityResolver identitiesResolver, AuthenticatorsRegistry reg, 
			String name, String typeId, String rConfiguration, String localCredentialName, 
			String localCredentialconfiguration, int revision) throws WrongArgumentException
	{
		this(identitiesResolver, reg, name, revision);
		AuthenticatorTypeDescription authDesc = authRegistry.getAuthenticatorsById(typeId);
		if (authDesc == null)
			throw new WrongArgumentException("The authenticator type " + typeId + " is not known");
		createCoworkers(authDesc, rConfiguration, localCredentialconfiguration, localCredentialName);
	}
	
	/**
	 * For initial object creation in case of remote authenticator.
	 * @param reg
	 * @param typeId
	 * @param configuration
	 * @throws WrongArgumentException 
	 */
	public AuthenticatorImpl(IdentityResolver identitiesResolver, AuthenticatorsRegistry reg, 
			String name, String typeId, String rConfiguration, String vConfiguration, long revision)
					throws WrongArgumentException
	{
		this(identitiesResolver, reg, name, revision);
		AuthenticatorTypeDescription authDesc = authRegistry.getAuthenticatorsById(typeId);
		if (authDesc == null)
			throw new WrongArgumentException("The authenticator type '" + typeId + "' is invalid. "
					+ "Valid authenticator types are: " + authRegistry.getAuthenticatorTypes());
		createCoworkers(authDesc, rConfiguration, vConfiguration, null);
	}	
	
	/**
	 * Used when the state is initialized from persisted storage, for the remote authenticators.
	 * @param identitiesResolver coworker
	 * @param reg coworker
	 * @param name authenticator name
	 * @param deserialized deserialized state
	 */
	public AuthenticatorImpl(IdentityResolver identitiesResolver, AuthenticatorsRegistry reg, String name,
			AuthenticatorInstance deserialized)
	{
		this(identitiesResolver, reg, name, deserialized.getRevision());
		createCoworkers(deserialized.getTypeDescription(), deserialized.getRetrievalConfiguration(),
				deserialized.getVerificatorConfiguration(), null);
	}
	
	/**
	 * Used when the state is initialized from persisted storage, for the local authenticators.
	 * @param identitiesResolver coworker
	 * @param reg coworker
	 * @param name authenticator name
	 * @param deserialized deserialized state
	 * @param localCredentialConfiguration configuration of the local credential associated with the validator
	 */
	public AuthenticatorImpl(IdentityResolver identitiesResolver, AuthenticatorsRegistry reg, String name,
			AuthenticatorInstance deserialized, String localCredentialConfiguration)
	{
		this(identitiesResolver, reg, name, deserialized.getRevision());
		createCoworkers(deserialized.getTypeDescription(), deserialized.getRetrievalConfiguration(),
				localCredentialConfiguration, deserialized.getLocalCredentialName());
	}
	
	private AuthenticatorImpl(IdentityResolver identitiesResolver, AuthenticatorsRegistry reg, String name, long revision)
	{
		this.authRegistry = reg;
		this.instanceDescription = new AuthenticatorInstance();
		instanceDescription.setRevision(revision);
		this.instanceDescription.setId(name);
		this.identitiesResolver = identitiesResolver;
		
	}	
	
	private void createCoworkers(AuthenticatorTypeDescription authDesc, String rConfiguration, 
			String vConfiguration, String localCredential)
	{
		CredentialRetrievalFactory retrievalFact = authRegistry.getCredentialRetrievalFactory(
				authDesc.getRetrievalMethod());
		CredentialVerificatorFactory verificatorFact = authRegistry.getCredentialVerificatorFactory(
				authDesc.getVerificationMethod());
		verificator = verificatorFact.newInstance();
		verificator.setIdentityResolver(identitiesResolver);
		verificator.setInstanceName(instanceDescription.getId());
		retrieval = retrievalFact.newInstance();
		retrieval.setCredentialExchange(verificator, instanceDescription.getId());
		updateConfiguration(rConfiguration, vConfiguration, localCredential);
		instanceDescription.setTypeDescription(authDesc);
	}
	
	/**
	 * Updates the current configuration of the authenticator. 
	 * For local verificators the verificator configuration is only set for the underlying verificator, it is not
	 * exposed in the instanceDescription. 
	 * @param rConfiguration
	 * @param vConfiguration
	 */
	public void updateConfiguration(String rConfiguration, String vConfiguration, String localCredential)
	{
		if (rConfiguration == null)
			rConfiguration = "";
		retrieval.setSerializedConfiguration(rConfiguration);
		instanceDescription.setRetrievalConfiguration(rConfiguration);
		verificator.setSerializedConfiguration(vConfiguration);
		if (!(verificator instanceof LocalCredentialVerificator))
		{
			instanceDescription.setVerificatorConfiguration(vConfiguration);
		} else 
		{
			instanceDescription.setVerificatorConfiguration(null);
			((LocalCredentialVerificator)verificator).setCredentialName(localCredential);
			instanceDescription.setLocalCredentialName(localCredential);
		}
	}
	
	public void setRevision(long revision)
	{
		instanceDescription.setRevision(revision);
	}
	
	public long getRevision()
	{
		return instanceDescription.getRevision();
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
