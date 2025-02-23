/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.describedObject.DescribedObject;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.engine.api.utils.StringConfigurable;

/**
 * Implementations allow for verification of the provided credential. It is assumed that credential is 
 * provided via {@link CredentialExchange} interfaces. The actual interaction might be arbitrary complex.
 * <p>
 * Implementations must be thread safe. 
 * 
 * @author K. Benedyczak
 */
public interface CredentialVerificator extends CredentialExchange, DescribedObject, StringConfigurable
{
	public enum VerificatorType
	{
		Local, Remote, Mixed
	};
	
	void setIdentityResolver(IdentityResolver identityResolver);
	
	/**
	 * Instance name is the configured name of the authenticator to which this credential verificator belongs.
	 */
	void setInstanceName(String name);

	VerificatorType getType();
	
	AuthenticationMethod getAuthenticationMethod();
}
