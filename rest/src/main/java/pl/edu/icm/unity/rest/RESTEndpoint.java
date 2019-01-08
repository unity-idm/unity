/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest;

import java.io.StringReader;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;

import javax.servlet.DispatcherType;
import javax.ws.rs.core.Application;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.JAXRSBindingFactory;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.endpoint.AbstractWebEndpoint;
import pl.edu.icm.unity.engine.api.endpoint.BindingAuthn;
import pl.edu.icm.unity.engine.api.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.rest.authn.AuthenticationInterceptor;
import pl.edu.icm.unity.rest.authn.CXFAuthentication;
import pl.edu.icm.unity.rest.exception.EngineExceptionMapper;
import pl.edu.icm.unity.rest.exception.IllegalArgumentExceptionMapper;
import pl.edu.icm.unity.rest.exception.InternalExceptionMapper;
import pl.edu.icm.unity.rest.exception.JSONExceptionMapper;
import pl.edu.icm.unity.rest.exception.JSONParseExceptionMapper;
import pl.edu.icm.unity.rest.exception.JSONParsingExceptionMapper;
import pl.edu.icm.unity.rest.exception.NPEExceptionMapper;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;

/**
 * JAX-RS (REST) endpoint based on CXF.
 * The implementations are required to implement a single method {@link #getApplication()}. The returned object must
 * extend JAX-RS {@link Application} and return all endpoint specific classes and/or objects. Note that the 
 * returned {@link Application} will be enriched with additional Unity-specific filters/interceptors 
 * to handle authentication.
 * 
 *  
 * @author K. Benedyczak
 */
public abstract class RESTEndpoint extends AbstractWebEndpoint implements WebAppEndpointInstance
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, RESTEndpoint.class);
	private AuthenticationProcessor authenticationProcessor;
	protected RESTEndpointProperties genericEndpointProperties;
	protected String servletPath;
	protected SessionManagement sessionMan;
	protected UnityMessageSource msg;
	
	protected Set<String> notProtectedPaths = new HashSet<String>();
	
	public RESTEndpoint(UnityMessageSource msg, SessionManagement sessionMan, 
			AuthenticationProcessor authenticationProcessor,
			NetworkServer server, String servletPath)
	{
		super(server);
		this.authenticationProcessor = authenticationProcessor;
		this.servletPath = servletPath;
		this.msg = msg;
		this.sessionMan = sessionMan;
	}

	@Override
	protected void setSerializedConfiguration(String serializedState)
	{
		properties = new Properties();
		try
		{
			properties.load(new StringReader(serializedState));
			genericEndpointProperties = new RESTEndpointProperties(properties);
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the the generic RESTful"
					+ " endpoint's configuration", e);
		}		
	}

	/**
	 * @param paths paths that should have the anonymous access. The paths are relative to the endpoint 
	 * address (i.e. should start with the servlet's address).
	 */
	protected void addNotProtectedPaths(String... paths)
	{
		for (String path: paths)
			notProtectedPaths.add(description.getEndpoint().getContextAddress() + path);
	}
	
	protected abstract Application getApplication();

	private void deployResources(Bus bus)
	{
		JAXRSServerFactoryBean sf = ResourceUtils.createApplication(getApplication(), false);
		sf.setBus(bus);
		
		JAXRSBindingFactory factory = new JAXRSBindingFactory();
		factory.setBus(bus);

		BindingFactoryManager manager = bus.getExtension(BindingFactoryManager.class);
		manager.registerBindingFactory(JAXRSBindingFactory.JAXRS_BINDING_ID, factory);
		
		Server server = sf.create();
		
		Endpoint cxfEndpoint = server.getEndpoint();
		addInterceptors(cxfEndpoint.getInInterceptors(), cxfEndpoint.getOutInterceptors());
	}
	
	@Override
	public ServletContextHandler getServletContextHandler()
	{
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		context.setContextPath(description.getEndpoint().getContextAddress());
		
		CXFNonSpringServlet cxfServlet = new CXFNonSpringServlet();
		Bus bus = BusFactory.newInstance().createBus();
		cxfServlet.setBus(bus);
		
		ServletHolder servlet = new ServletHolder(cxfServlet);
		servlet.setName("services");
		servlet.setForcedPath("services");
		context.addServlet(servlet, servletPath + "/*");

		if (!genericEndpointProperties.getListOfValues(RESTEndpointProperties.ENABLED_CORS_ORIGINS).isEmpty())
			context.addFilter(getCorsFilter(), servletPath + "/*", EnumSet.of(DispatcherType.REQUEST));
		
		deployResources(bus);
		return context;
	}
	
	protected FilterHolder getCorsFilter()
	{
		CrossOriginFilter filter = new CrossOriginFilter();
		FilterHolder filterHolder = new FilterHolder(filter);
		List<String> allowedOrigins = 
				genericEndpointProperties.getListOfValues(RESTEndpointProperties.ENABLED_CORS_ORIGINS);
		StringJoiner originsJoiner = new StringJoiner(",");
		allowedOrigins.forEach(origin -> originsJoiner.add(origin));

		List<String> allowedHeaders = 
				genericEndpointProperties.getListOfValues(RESTEndpointProperties.ENABLED_CORS_HEADERS);
		StringJoiner headersJoiner = new StringJoiner(",");
		allowedHeaders.forEach(origin -> headersJoiner.add(origin));
		String allowedHeadersSpec = allowedHeaders.isEmpty() ? "*" : headersJoiner.toString();
		
		filterHolder.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, allowedHeadersSpec);
		filterHolder.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, originsJoiner.toString());
		filterHolder.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,HEAD,DELETE,PUT,OPTIONS");
		
		log.debug("Will allow CORS for the following origins: " + originsJoiner.toString());
		
		return filterHolder;
	}
	
	@Override
	public void updateAuthenticationFlows(List<AuthenticationFlow> authenticators)
			throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();
	}
	
	private void addInterceptors(List<Interceptor<? extends Message>> inInterceptors,
			List<Interceptor<? extends Message>> outInterceptors)
	{
		AuthenticationRealm realm = description.getRealm();
		inInterceptors.add(new AuthenticationInterceptor(msg, authenticationProcessor, 
				authenticationFlows, realm, sessionMan, notProtectedPaths,
				getEndpointDescription().getType().getFeatures()));
		installAuthnInterceptors(authenticationFlows, inInterceptors);
	}

	public static void installAuthnInterceptors(List<AuthenticationFlow> authenticatorFlows,
			List<Interceptor<? extends Message>> interceptors)
	{
		Set<String> added = new HashSet<>();
		for (AuthenticationFlow authenticatorFlow: authenticatorFlows)
		{
			for (AuthenticatorInstance authenticator : authenticatorFlow.getAllAuthenticators())
			{	
				installAuthnInterceptor(authenticator.getRetrieval(), interceptors, added);
			}
		}
	}

	private static void installAuthnInterceptor(BindingAuthn authenticator, 
			List<Interceptor<? extends Message>> interceptors, Set<String> added)
	{
		if (!added.contains(authenticator.getAuthenticatorId()))
		{
			CXFAuthentication a = (CXFAuthentication) authenticator;
			Interceptor<? extends Message> in = a.getInterceptor();
			if (in != null)
				interceptors.add(in);
			added.add(authenticator.getAuthenticatorId());
		}
	}
	
	/**
	 * Adds common exception handlers
	 */
	public static void installExceptionHandlers(HashSet<Object> ret)
	{
		ret.add(new EngineExceptionMapper());
		ret.add(new NPEExceptionMapper());
		ret.add(new IllegalArgumentExceptionMapper());
		ret.add(new InternalExceptionMapper());
		ret.add(new JSONParseExceptionMapper());
		ret.add(new JSONParsingExceptionMapper());
		ret.add(new JSONExceptionMapper());
	}
}
