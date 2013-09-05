/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mock;

import java.util.List;
import java.util.Map;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.endpoint.AbstractEndpoint;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.endpoint.WebAppEndpointInstance;

public class MockEndpoint extends AbstractEndpoint implements WebAppEndpointInstance
{
	public MockEndpoint()
	{
		super(MockEndpointFactory.TYPE);
	}

	@Override
	public String getSerializedConfiguration()
	{
		return "";
	}

	@Override
	protected void setSerializedConfiguration(String json)
	{
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
		MockBinding authenticator = (MockBinding)authenticators.get(0).values().iterator().next();
		return authenticator.authenticate();
	}

	@Override
	public void updateAuthenticators(List<Map<String, BindingAuthn>> authenticators)
	{
		this.authenticators = authenticators;
	}
}
