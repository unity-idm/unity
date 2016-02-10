/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mock;

import java.util.List;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.server.endpoint.AbstractWebEndpoint;
import pl.edu.icm.unity.server.endpoint.WebAppEndpointInstance;

public class MockEndpoint extends AbstractWebEndpoint implements WebAppEndpointInstance
{
	public static final String WRONG_CONFIG = "wrong";
	
	public MockEndpoint(NetworkServer httpServer)
	{
		super(httpServer);
	}

	@Override
	public String getSerializedConfiguration()
	{
		return "";
	}

	@Override
	protected void setSerializedConfiguration(String json)
	{
		if (json.equals(WRONG_CONFIG))
			throw new IllegalStateException("Wrong configuration");
	}

	@Override
	public ServletContextHandler getServletContextHandler()
	{
		ServletContextHandler ret = new ServletContextHandler();
		ret.setContextPath(description.getContextAddress());
		ret.addServlet(DefaultServlet.class, "/");
		return ret;
	}
	
	public Long authenticate() throws EngineException
	{
		MockBinding authenticator = (MockBinding)authenticators.get(0).getPrimaryAuthenticator();
		return authenticator.authenticate();
	}

	@Override
	public void updateAuthenticationOptions(List<AuthenticationOption> authenticators)
	{
		this.authenticators = authenticators;
	}
}
