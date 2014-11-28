/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import java.util.Date;

import pl.edu.icm.unity.server.api.internal.LoginSession;

/**
 * Common code for {@link SAMLExternalLogoutContext} and {@link SAMLInternalLogoutContext}
 * @author K. Benedyczak
 */
public abstract class AbstractSAMLLogoutContext
{
	protected Date creationTs;
	protected LoginSession session;
	
	public AbstractSAMLLogoutContext(LoginSession session)
	{
		this.creationTs = new Date();
		this.session = session;
	}

	public Date getCreationTs()
	{
		return creationTs;
	}

	public LoginSession getSession()
	{
		return session;
	}
	
	@Override
	public String toString()
	{
		return "Logout request for " + session;
	}
}
