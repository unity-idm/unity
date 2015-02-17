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

import pl.edu.icm.unity.rest.RESTEndpoint;
import pl.edu.icm.unity.rest.authn.AuthenticationInterceptor;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.server.authn.AuthenticationProcessor;
import pl.edu.icm.unity.server.endpoint.AbstractEndpoint;
import pl.edu.icm.unity.server.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * Web service endpoint based on CXF
 * @author K. Benedyczak
 */
public abstract class CXFEndpoint extends AbstractEndpoint implements WebAppEndpointInstance
{
	protected UnityMessageSource msg;
	protected String servletPath;
	private Map<Class<?>, Object> services; 
	protected CXFEndpointProperties genericEndpointProperties;
	protected SessionManagement sessionMan;
	private AuthenticationProcessor authnProcessor;
	
	public CXFEndpoint(UnityMessageSource msg, SessionManagement sessionMan, AuthenticationProcessor authnProcessor,
			EndpointTypeDescription type, String servletPath)
	{
		super(type);
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.servletPath = servletPath;
		this.sessionMan = sessionMan;
		services = new HashMap<Class<?>, Object>();
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
		inInterceptors.add(new AuthenticationInterceptor(msg, authnProcessor, authenticators, realm, sessionMan, 
				new HashSet<String>()));
		RESTEndpoint.installAuthnInterceptors(authenticators, inInterceptors);
	}
	
	protected abstract void configureServices();
	
	@Override
	public ServletContextHandler getServletContextHandler()
	{
		configureServices();

		Bus bus = BusFactory.newInstance().createBus();

		ServletContextHandler context = CXFUtils.getServletContextHandler(
				description.getContextAddress(), servletPath, bus);
		
		for (Map.Entry<Class<?>, Object> service: services.entrySet())
			deployWebservice(bus, service.getKey(), service.getValue());
		
		return context;
	}
	

	@Override
	public void updateAuthenticationOptions(List<AuthenticationOption> authenticators)
	{
		throw new UnsupportedOperationException();
	}
}
