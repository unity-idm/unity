/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mock;

import java.util.List;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.server.endpoint.AbstractEndpoint;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

public class MockEndpoint extends AbstractEndpoint implements WebAppEndpointInstance
{
	private ObjectMapper mapper = new ObjectMapper();
	
	public MockEndpoint()
	{
		super(MockEndpointFactory.TYPE);
	}

	@Override
	public JsonNode getSerializedConfiguration()
	{
		ObjectNode root = mapper.createObjectNode();
		try
		{
			String val = mapper.writeValueAsString(description);
			root.put("description", mapper.readTree(val));
		} catch (Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException("can't serialize", e);
		}
		return root;
	}

	@Override
	public void setSerializedConfiguration(JsonNode json)
	{
		ObjectNode root = (ObjectNode) json;
		JsonNode desc = root.get("description");
		try
		{
			String jsonStr = mapper.writeValueAsString(desc);
			description = mapper.readValue(jsonStr, EndpointDescription.class);
		} catch (Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setAuthenticators(List<AuthenticatorSet> authenticatorsInfo,
			List<BindingAuthn> authenticators)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public ServletContextHandler getServletContextHandler()
	{
		ServletContextHandler ret = new ServletContextHandler();
		ret.setContextPath(description.getContextAddress());
		ret.addServlet(DefaultServlet.class, "/");
		return ret;
	}
}
