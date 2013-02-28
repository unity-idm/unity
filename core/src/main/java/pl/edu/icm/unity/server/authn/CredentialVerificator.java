/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import pl.edu.icm.unity.types.DescribedObject;
import pl.edu.icm.unity.types.JsonSerializable;

/**
 * Implementations allow for verification of the provided credential. It is assumed that credential is 
 * provided in an asynchronous way, via callback. The actual interaction might be arbitrary complex and
 * is implemented using {@link CredentialExchange}. 
 * 
 * @author K. Benedyczak
 */
public interface CredentialVerificator extends JsonSerializable, DescribedObject
{
	public String getRequiredExchangerId();
	public void setCredentialExchanger(CredentialExchange e);
}
