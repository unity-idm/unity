/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import pl.edu.icm.unity.server.endpoint.BindingAuthn;

/**
 * Contract of credential retrieval used by an LDAP server runtime.
 * @author K. Benedyczak
 */
public interface LdapServerAuthentication extends BindingAuthn
{
	String NAME = "raw-password-auth";
}
