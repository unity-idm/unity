/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import pl.edu.icm.unity.types.DescribedObject;


/**
 * Produces {@link CredentialVerificator}s of a particular type.
 * @author K. Benedyczak
 */
public interface CredentialVerificatorFactory extends DescribedObject
{
	public CredentialVerificator newInstance();
}
