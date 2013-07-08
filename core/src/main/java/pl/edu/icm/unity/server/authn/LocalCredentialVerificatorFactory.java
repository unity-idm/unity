/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;


/**
 * Produces {@link LocalCredentialVerificator}s of a particular type.
 * @author K. Benedyczak
 */
public interface LocalCredentialVerificatorFactory extends CredentialVerificatorFactory
{
	public LocalCredentialVerificator newInstance();
	
	public boolean isSupportingInvalidation();
}
