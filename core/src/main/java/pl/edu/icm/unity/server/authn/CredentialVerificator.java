/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.types.DescribedObject;
import pl.edu.icm.unity.types.JsonSerializable;

/**
 * Implementations allow for verification of the provided credential. It is assumed that credential is 
 * provided via {@link CredentialExchange} interfaces. The actual interaction might be arbitrary complex.
 * <p>
 * Implementations must be thread safe. 
 * 
 * @author K. Benedyczak
 */
public interface CredentialVerificator extends CredentialExchange, JsonSerializable, DescribedObject
{
	public void setIdentityResolver(IdentityResolver identityResolver);
}
