/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import java.util.Properties;

/**
 *
 * @author Willem Elbers <willem@clarin.eu>
 */
public class LdapServerFacadeMockFactory
{

	public static LdapServerFacade create()
	{
		String host = "localhost";
		int port = 10000;
		String name = null;
		String workdir = "/tmp/unity-test";
		return new LdapServerFacade(host, port, name, workdir);
	}

	public static LdapServerFacade getNullMock() throws Exception
	{
		Properties cfg = new Properties();
		cfg.setProperty("unity.ldapServer.userNameAliases", "uid");
		cfg.setProperty("unity.ldapServer.credential", "MAIN");
		LdapServerProperties props = new LdapServerProperties(cfg);
		LdapApacheDSInterceptor ladi = new LdapApacheDSInterceptor(null, null, null, null,
				null, props, null, null);
		LdapServerFacade facade = create();
		facade.init(true, ladi, null);
		return facade;
	}

	public static LdapServerFacade getMockWithServerProperties(LdapServerProperties props)
			throws Exception
	{
		LdapApacheDSInterceptor ladi = new LdapApacheDSInterceptor(null, null, null, null,
				null, props, null, null);
		LdapServerFacade facade = create();
		facade.init(true, ladi, null);
		return facade;
	}
}
