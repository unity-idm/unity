/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mock;

import java.util.Collections;
import java.util.List;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.endpoint.AbstractWebEndpoint;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

@PrototypeComponent
public class MockEndpoint extends AbstractWebEndpoint implements WebAppEndpointInstance
{
	public static final String NAME = "Mock Endpoint";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(
			NAME, "This is mock endpoint for tests", "web",
			Collections.singletonMap("endPaths", "descEndPaths"));

	public static final String WRONG_CONFIG = "wrong";
	
	@Autowired
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
		ret.setContextPath(description.getEndpoint().getContextAddress());
		ret.addServlet(DefaultServlet.class, "/");
		return ret;
	}
	
	public Long authenticate() throws EngineException
	{
		AuthenticatorInstance authenticator =  authenticationFlows.get(0)
				.getFirstFactorAuthenticators().iterator().next();	
		return ((MockBinding)authenticator.getRetrieval()).authenticate();
	}

	@Override
	public void updateAuthenticationFlows(List<AuthenticationFlow> authenticators)
	{
		this.authenticationFlows = authenticators;
	}
	
	@Component
	public static class Factory implements EndpointFactory
	{
		@Autowired
		private ObjectFactory<MockEndpoint> factory;
		
		@Override
		public EndpointTypeDescription getDescription()
		{
			return TYPE;
		}

		@Override
		public EndpointInstance newInstance()
		{
			return factory.getObject();
		}
	}
}
