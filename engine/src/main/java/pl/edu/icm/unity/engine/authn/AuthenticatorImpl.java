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
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator.VerificatorType;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificator;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.store.types.AuthenticatorConfiguration;
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
 * TODO move logic to AuthenticatorLoader
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
	 */
	public AuthenticatorImpl(IdentityResolver identitiesResolver, AuthenticatorsRegistry reg, 
			String name, String typeId, String localCredentialName, 
			String localCredentialconfiguration, String binding) throws WrongArgumentException
	{
		this(identitiesResolver, reg, name, 0);
		//FIXME x2 getting authTypeDesc is actually not needed here, rather checking if we have such verificator, 
		//but thats done later on anyway
		AuthenticatorTypeDescription authDesc = authRegistry.getAuthenticatorsById(typeId);
		if (authDesc == null)
			throw new WrongArgumentException("The authenticator type " + typeId + " is not known");
		createCoworkers(authDesc.getVerificationMethod(), localCredentialconfiguration, localCredentialName, binding);
	}
	
	/**
	 * For initial object creation in case of remote authenticator.
	 */
	public AuthenticatorImpl(IdentityResolver identitiesResolver, AuthenticatorsRegistry reg, 
			String name, String typeId, String vConfiguration, String binding)
					throws WrongArgumentException
	{
		this(identitiesResolver, reg, name, 0);
		AuthenticatorTypeDescription authDesc = authRegistry.getAuthenticatorsById(typeId);
		if (authDesc == null)
			throw new WrongArgumentException("The authenticator type '" + typeId + "' is invalid. "
					+ "Valid authenticator types are: " + authRegistry.getAuthenticatorTypes());
		createCoworkers(authDesc.getVerificationMethod(), vConfiguration, null, binding);
	}	
	
	/**
	 * Used when the state is initialized from persisted storage, for the remote authenticators.
	 * @param identitiesResolver coworker
	 * @param reg coworker
	 * @param name authenticator name
	 * @param deserialized deserialized state
	 */
	public AuthenticatorImpl(IdentityResolver identitiesResolver, AuthenticatorsRegistry reg, String name,
			AuthenticatorConfiguration deserialized, String binding)
	{
		this(identitiesResolver, reg, name, deserialized.getRevision());
		createCoworkers(deserialized.getVerificationMethod(), deserialized.getConfiguration(),
				null, binding);
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
			AuthenticatorConfiguration deserialized, String localCredentialConfiguration, String binding)
	{
		this(identitiesResolver, reg, name, deserialized.getRevision());
		createCoworkers(deserialized.getVerificationMethod(), localCredentialConfiguration, 
				deserialized.getLocalCredentialName(), binding);
	}
	
	private AuthenticatorImpl(IdentityResolver identitiesResolver, AuthenticatorsRegistry reg, String name, long revision)
	{
		this.authRegistry = reg;
		this.instanceDescription = new AuthenticatorInstance();
		instanceDescription.setRevision(revision);
		this.instanceDescription.setId(name);
		this.identitiesResolver = identitiesResolver;
		
	}	
	
	private void createCoworkers(String verificationMethod, String configuration, String localCredential, 
			String binding)
	{
		CredentialVerificatorFactory verificatorFact = authRegistry.getCredentialVerificatorFactory(
				verificationMethod);
		verificator = verificatorFact.newInstance();
		verificator.setIdentityResolver(identitiesResolver);
		verificator.setInstanceName(instanceDescription.getId());
		
		CredentialRetrievalFactory retrievalFact = authRegistry.findCredentialRetrieval(binding, verificator);
		retrieval = retrievalFact.newInstance();
		retrieval.setCredentialExchange(verificator, instanceDescription.getId());
		updateConfiguration(configuration, localCredential);
		AuthenticatorTypeDescription authDesc = getAuthenticatorDescription(verificator, 
				localCredential != null);
		instanceDescription.setTypeDescription(authDesc);
	}
	
	//FIXME - id and verificator id the same.
	private AuthenticatorTypeDescription getAuthenticatorDescription(CredentialVerificator verificator, 
			boolean isLocal)
	{
		return new AuthenticatorTypeDescription(verificator.getName(), 
				verificator.getName(), 
				verificator.getDescription(), 
				isLocal);
	}
	
	/**
	 * Updates the current configuration of the authenticator. 
	 * For local verificators the verificator configuration is only set for the underlying verificator, it is not
	 * exposed in the instanceDescription. 
	 */
	@Override
	public void updateConfiguration(String configuration, String localCredential)
	{
		retrieval.setSerializedConfiguration(configuration);
		verificator.setSerializedConfiguration(configuration);
		if (!(verificator.getType().equals(VerificatorType.Local)))
		{
			instanceDescription.setVerificatorConfiguration(configuration);
		} else 
		{
			instanceDescription.setVerificatorConfiguration(null);
			((LocalCredentialVerificator)verificator).setCredentialName(localCredential);
			instanceDescription.setLocalCredentialName(localCredential);
		}
	}
	
	@Override
	public void setRevision(long revision)
	{
		instanceDescription.setRevision(revision);
	}
	
	@Override
	public long getRevision()
	{
		return instanceDescription.getRevision();
	}
		
	@Override
	public AuthenticatorInstance getAuthenticatorInstance()
	{
		return instanceDescription;
	}

	@Override
	public CredentialRetrieval getRetrieval()
	{
		return retrieval;
	}
}
