/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.authn.CredentialRetrievalFactory;
import pl.edu.icm.unity.server.authn.CredentialVerificator;
import pl.edu.icm.unity.server.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificator;
import pl.edu.icm.unity.server.registries.AuthenticatorsRegistry;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;

/**
 * Internal representation of an authenticator, which is a composition of {@link CredentialRetrieval} and
 * {@link CredentialVerificator}, configured.
 * <p>
 * Authenticator can be local or remote, depending on the associated verificator type (local or remote).
 * <p>
 * Local authenticator is special as it has an associated local credential. Its verificator uses the associated 
 * credential's configuration internally, but it is not advertised to the outside world, via the
 * {@link AuthenticatorInstance} interface.
 * <p>
 * Instantiation can be done in two scenarios, each in two variants:
 * <ul>
 * <li>either a constructor is called with a state loaded previously from persisted storage and provided in    
 * {@link AuthenticatorImpl#setAuthenticatorInstance(AuthenticatorInstance)}. If the authenticator is local, 
 * then the local credential must be separately given.
 * <li> Otherwise a full constructor is called to initialize the object completely. In case of a local authenticator
 * a local credential name and its configuration must be provided.
 * </ul>
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
	 * For initial object creation in case of local authenticator.
	 * @param reg
	 * @param typeId
	 * @param configuration
	 * @throws WrongArgumentException 
	 */
	public AuthenticatorImpl(IdentityResolver identitiesResolver, AuthenticatorsRegistry reg, 
			String name, String typeId, String rConfiguration, String localCredentialName, 
			String localCredentialconfiguration) throws WrongArgumentException
	{
		this(identitiesResolver, reg, name);
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
			String name, String typeId, String rConfiguration, String vConfiguration)
					throws WrongArgumentException
	{
		this(identitiesResolver, reg, name);
		AuthenticatorTypeDescription authDesc = authRegistry.getAuthenticatorsById(typeId);
		if (authDesc == null)
			throw new WrongArgumentException("The authenticator type " + typeId + " is not known");
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
		this(identitiesResolver, reg, name);
		createCoworkers(deserialized.getTypeDescription(), deserialized.getRetrievalJsonConfiguration(),
				deserialized.getVerificatorJsonConfiguration(), null);
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
		this(identitiesResolver, reg, name);
		createCoworkers(deserialized.getTypeDescription(), deserialized.getRetrievalJsonConfiguration(),
				localCredentialConfiguration, deserialized.getLocalCredentialName());
	}
	
	private AuthenticatorImpl(IdentityResolver identitiesResolver, AuthenticatorsRegistry reg, String name)
	{
		this.authRegistry = reg;
		this.instanceDescription = new AuthenticatorInstance();
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
		retrieval = retrievalFact.newInstance();
		retrieval.setCredentialExchange(verificator);
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
		retrieval.setSerializedConfiguration(rConfiguration);
		instanceDescription.setRetrievalJsonConfiguration(rConfiguration);
		verificator.setSerializedConfiguration(vConfiguration);
		if (!(verificator instanceof LocalCredentialVerificator))
		{
			instanceDescription.setVerificatorJsonConfiguration(vConfiguration);
		} else 
		{
			instanceDescription.setVerificatorJsonConfiguration(null);
			((LocalCredentialVerificator)verificator).setCredentialName(localCredential);
			instanceDescription.setLocalCredentialName(localCredential);
		}
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
