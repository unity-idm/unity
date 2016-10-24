/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.shared.DefaultCoreSession;

import pl.edu.icm.unity.server.api.internal.LoginSession;

public class CoreSessionExt extends DefaultCoreSession
{
	private LoginSession session;

	public CoreSessionExt(LdapPrincipal principal, DirectoryService directoryService, LoginSession session)
	{
		super(principal, directoryService);
		this.session = session;
	}

	public LoginSession getSession()
	{
		return session;
	}
}
