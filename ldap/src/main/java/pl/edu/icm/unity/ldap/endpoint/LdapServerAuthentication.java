/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;

import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;

/**
 * Contract of credential retrieval used by an LDAP server runtime.
 * @author K. Benedyczak
 */
public interface LdapServerAuthentication extends BindingAuthn
{
	String NAME = "ldap-protocol";
	
	AuthenticationResult authenticate(BindOperationContext bindContext);
}
