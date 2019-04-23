/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import pl.edu.icm.unity.types.DescribedObject;

/**
 * Implementations allows for creating {@link CredentialRetrieval}s of a particular type.
 * 
 * @author K. Benedyczak
 */
public interface CredentialRetrievalFactory extends DescribedObject
{
	CredentialRetrieval newInstance();
	String getSupportedBinding();
	boolean isCredentialExchangeSupported(String credentialExchangeId);
}
