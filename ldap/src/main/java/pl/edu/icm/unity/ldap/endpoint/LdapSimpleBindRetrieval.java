/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import java.nio.charset.StandardCharsets;

import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;

import pl.edu.icm.unity.server.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.stdext.credential.PasswordExchange;

/**
 * Retrieves passwords from LDAP simple bind request and checks it against the configured verificator.
 * 
 */
public class LdapSimpleBindRetrieval extends AbstractCredentialRetrieval<PasswordExchange> 
		implements LdapServerAuthentication
{
	public LdapSimpleBindRetrieval()
	{
		super(NAME);
	}

	@Override
	public String getSerializedConfiguration()
	{
		return "";
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
		return;
	}

	@Override
	public AuthenticationResult authenticate(LdapServerProperties configuration, BindOperationContext bindContext)
	{
		String username = LdapNodeUtils.getUserName(configuration, bindContext.getDn());
		String password = new String(bindContext.getCredentials(), StandardCharsets.UTF_8);
		return credentialExchange.checkPassword(username, password, null);
	}
}
