/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.local;

import pl.edu.icm.unity.engine.api.authn.CredentialVerificatorFactory;

/**
 * Produces {@link LocalCredentialVerificator}s of a particular type.
 * @author K. Benedyczak
 */
public interface LocalCredentialVerificatorFactory extends CredentialVerificatorFactory
{
	@Override
	LocalCredentialVerificator newInstance();
	
	boolean isSupportingInvalidation();
}
