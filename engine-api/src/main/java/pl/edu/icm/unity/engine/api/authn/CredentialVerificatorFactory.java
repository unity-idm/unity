/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import pl.edu.icm.unity.base.describedObject.DescribedObject;


/**
 * Produces {@link CredentialVerificator}s of a particular type.
 * @author K. Benedyczak
 */
public interface CredentialVerificatorFactory extends DescribedObject
{
	CredentialVerificator newInstance();
}
