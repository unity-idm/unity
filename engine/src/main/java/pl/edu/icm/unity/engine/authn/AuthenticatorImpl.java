/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator.VerificatorType;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificator;
import pl.edu.icm.unity.types.authn.AuthenticatorInstanceMetadata;

/**
 * Runtime instance of authenticator, with a selected retrieval. Stateful and mutable.
 * @author P.Piernik
 */
class AuthenticatorImpl implements AuthenticatorInstance
{
	private CredentialRetrieval retrieval;
	private CredentialVerificator verificator;
	private AuthenticatorInstanceMetadata instanceDescription;
	
	AuthenticatorImpl(CredentialRetrieval retrieval, CredentialVerificator verificator,
			AuthenticatorInstanceMetadata instanceDescription)
	{
		this.retrieval = retrieval;
		this.verificator = verificator;
		this.instanceDescription = instanceDescription;
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
			instanceDescription.setConfiguration(configuration);
		} else 
		{
			instanceDescription.setConfiguration(null);
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
	public AuthenticatorInstanceMetadata getMetadata()
	{
		return instanceDescription;
	}

	@Override
	public CredentialRetrieval getRetrieval()
	{
		return retrieval;
	}
}
