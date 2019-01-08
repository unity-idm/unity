/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws;

import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.eclipse.jetty.servlet.ServletContextHandler;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.endpoint.AbstractWebEndpoint;
import pl.edu.icm.unity.engine.api.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.rest.RESTEndpoint;
import pl.edu.icm.unity.rest.authn.AuthenticationInterceptor;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;

/**
 * Web service endpoint based on CXF
 * @author K. Benedyczak
 */
public abstract class CXFEndpoint extends AbstractWebEndpoint implements WebAppEndpointInstance
{
	protected UnityMessageSource msg;
	protected String servletPath;
	private Map<Class<?>, Object> services; 
	protected CXFEndpointProperties genericEndpointProperties;
	protected SessionManagement sessionMan;
	private AuthenticationProcessor authnProcessor;
	
	public CXFEndpoint(UnityMessageSource msg, SessionManagement sessionMan, AuthenticationProcessor authnProcessor,
			NetworkServer server, String servletPath)
	{
		super(server);
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.servletPath = servletPath;
		this.sessionMan = sessionMan;
		services = new HashMap<>();
	}
	
	@Override
	public void setSerializedConfiguration(String cfg)
	{
		properties = new Properties();
		try
		{
			properties.load(new StringReader(cfg));
			genericEndpointProperties = new CXFEndpointProperties(properties);
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the the generic Web Service"
					+ " endpoint's configuration", e);
		}
	}

	protected void addWebservice(Class<?> iface, Object impl)
	{
		services.put(iface, impl);
	}
	
	private void deployWebservice(Bus bus, Class<?> iface, Object impl)
	{
		Endpoint cxfEndpoint = CXFUtils.deployWebservice(bus, iface, impl);
		addInterceptors(cxfEndpoint.getInInterceptors(), cxfEndpoint.getOutInterceptors());
	}
	
	private void addInterceptors(List<Interceptor<? extends Message>> inInterceptors,
			List<Interceptor<? extends Message>> outInterceptors)
	{
		outInterceptors.add(new XmlBeansNsHackOutHandler());
		AuthenticationRealm realm = description.getRealm();
		inInterceptors.add(new AuthenticationInterceptor(msg, authnProcessor, authenticationFlows, realm, sessionMan, 
				new HashSet<String>(), getEndpointDescription().getType().getFeatures()));
		RESTEndpoint.installAuthnInterceptors(authenticationFlows, inInterceptors);
	}
	
	protected abstract void configureServices();
	
	@Override
	public ServletContextHandler getServletContextHandler()
	{
		configureServices();

		Bus bus = BusFactory.newInstance().createBus();

		ServletContextHandler context = CXFUtils.getServletContextHandler(
				description.getEndpoint().getContextAddress(), servletPath, bus);
		
		for (Map.Entry<Class<?>, Object> service: services.entrySet())
			deployWebservice(bus, service.getKey(), service.getValue());
		
		return context;
	}
	

	@Override
	public void updateAuthenticationFlows(List<AuthenticationFlow> authenticators)
	{
		throw new UnsupportedOperationException();
	}
}
