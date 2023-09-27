/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest;

import eu.unicore.util.configuration.ConfigurationException;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.ee8.servlet.FilterHolder;
import org.eclipse.jetty.ee8.servlet.ServletContextHandler;
import org.eclipse.jetty.ee8.servlet.ServletHolder;
import org.eclipse.jetty.ee8.servlets.CrossOriginFilter;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.endpoint.AbstractWebEndpoint;
import pl.edu.icm.unity.engine.api.endpoint.BindingAuthn;
import pl.edu.icm.unity.engine.api.endpoint.WebAppEndpointEE8Instance;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.rest.authn.AuthenticationInterceptor;
import pl.edu.icm.unity.rest.authn.CXFAuthentication;
import pl.edu.icm.unity.rest.authn.LogContextCleaningInterceptor;

import javax.servlet.DispatcherType;
import javax.ws.rs.core.Application;
import java.io.StringReader;
import java.util.*;

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
public abstract class RESTEndpoint extends AbstractWebEndpoint implements WebAppEndpointEE8Instance
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, RESTEndpoint.class);
	private AuthenticationProcessor authenticationProcessor;
	protected RESTEndpointProperties genericEndpointProperties;
	protected String servletPath;
	protected SessionManagement sessionMan;
	protected MessageSource msg;
	protected final EntityManagement entityMan;

	protected Set<String> notProtectedPaths = new HashSet<>();
	protected Set<String> optionallyAuthenticatedPaths = new HashSet<>();
	
	public RESTEndpoint(MessageSource msg,
			SessionManagement sessionMan,
			AuthenticationProcessor authenticationProcessor,
			NetworkServer server,
			AdvertisedAddressProvider advertisedAddrProvider,
			String servletPath,
			EntityManagement entityMan)
	{
		super(server, advertisedAddrProvider);
		this.authenticationProcessor = authenticationProcessor;
		this.servletPath = servletPath;
		this.msg = msg;
		this.sessionMan = sessionMan;
		this.entityMan = entityMan;
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

	/**
	 * @param paths paths that should have optional authentication. The paths are relative to the endpoint 
	 * address (i.e. should start with the servlet's address). Requests to those paths will be authenticated 
	 * if possible but if no authentication material was given the request will proceed to the handler as an 
	 * anonymous request.
	 */
	protected void addOptionallyAuthenticatedPaths(String... paths)
	{
		for (String path: paths)
			optionallyAuthenticatedPaths.add(description.getEndpoint().getContextAddress() + path);
	}
	
	protected abstract Application getApplication();

	private void deployResources(Bus bus)
	{		
		Endpoint cxfEndpoint = RestEndpointHelper.createCxfEndpoint(getApplication(), bus);
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
	
	protected void addInterceptors(List<Interceptor<? extends Message>> inInterceptors,
			List<Interceptor<? extends Message>> outInterceptors)
	{
		AuthenticationRealm realm = description.getRealm();
		inInterceptors.add(new AuthenticationInterceptor(msg, authenticationProcessor, 
				authenticationFlows, realm, sessionMan, notProtectedPaths, optionallyAuthenticatedPaths,
				getEndpointDescription().getType().getFeatures(), entityMan));
		inInterceptors.add(new LogContextCleaningInterceptor());
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
}
