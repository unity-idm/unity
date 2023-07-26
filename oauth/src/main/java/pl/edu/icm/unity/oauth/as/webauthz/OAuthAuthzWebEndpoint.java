/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.openid.connect.sdk.OIDCError;
import com.vaadin.flow.server.startup.ServletContextListeners;
import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.vaadin.auth.VaadinAuthentication;
import io.imunity.vaadin.auth.server.AuthenticationFilter;
import io.imunity.vaadin.auth.server.ProxyAuthenticationFilter;
import io.imunity.vaadin.auth.server.SecureVaadin2XEndpoint;
import io.imunity.vaadin.endpoint.common.EopException;
import io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationPolicy;
import pl.edu.icm.unity.engine.api.authn.RememberMeProcessor;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.engine.api.utils.HiddenResourcesFilter;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.engine.api.utils.RoutingServlet;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;
import pl.edu.icm.unity.oauth.as.OAuthIdpStatisticReporter.OAuthIdpStatisticReporterFactory;
import pl.edu.icm.unity.oauth.as.OAuthScopesService;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.InvocationContextSetupFilter;
import pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilter;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

/**
 * OAuth2 authorization endpoint, Vaadin based.
 */
@PrototypeComponent
public class OAuthAuthzWebEndpoint extends SecureVaadin2XEndpoint
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, OAuthAuthzWebEndpoint.class);

	public static final String NAME = "OAuth2Authz";

	public static final String OAUTH_UI_SERVLET_PATH = "/oauth2-authz-web-ui";
	public static final String OAUTH_CONSUMER_SERVLET_PATH = "/oauth2-authz";
	public static final String OAUTH_ROUTING_SERVLET_PATH = "/oauth2-authz-web-entry";
	public static final String OAUTH_CONSENT_DECIDER_SERVLET_PATH = "/oauth2-authz-consentdecider";

	private final FreemarkerAppHandler freemarkerHandler;
	private final EntityManagement identitiesManagement;
	private final AttributesManagement attributesManagement;
	private final PKIManagement pkiManagement;
	private final OAuthEndpointsCoordinator coordinator;
	private final ASConsentDeciderServletFactory dispatcherServletFactory;
	private final OAuthIdpStatisticReporterFactory idpReporterFactory;

	private OAuthASProperties oauthProperties;

	private final OAuthScopesService scopeService;

	@Autowired
	public OAuthAuthzWebEndpoint(NetworkServer server,
			ApplicationContext applicationContext,
			FreemarkerAppHandler freemarkerHandler,
			@Qualifier("insecure") EntityManagement identitiesManagement,
			@Qualifier("insecure") AttributesManagement attributesManagement,
			PKIManagement pkiManagement,
			OAuthEndpointsCoordinator coordinator,
			ASConsentDeciderServletFactory dispatcherServletFactory,
			AdvertisedAddressProvider advertisedAddrProvider,
			MessageSource msg,
			RemoteRedirectedAuthnResponseProcessingFilter remoteAuthnResponseProcessingFilter,
			OAuthIdpStatisticReporterFactory idpReporterFactory,
			SandboxAuthnRouter sandboxAuthnRouter,
			OAuthScopesService scopeService)
	{
		super(server, advertisedAddrProvider, msg, applicationContext, new OAuthResourceProvider(),
				OAUTH_UI_SERVLET_PATH, remoteAuthnResponseProcessingFilter, sandboxAuthnRouter, OAuthVaadin2XServlet.class);
		this.freemarkerHandler = freemarkerHandler;
		this.attributesManagement = attributesManagement;
		this.identitiesManagement = identitiesManagement;
		this.pkiManagement = pkiManagement;
		this.coordinator = coordinator;
		this.dispatcherServletFactory = dispatcherServletFactory;
		this.idpReporterFactory = idpReporterFactory;
		this.scopeService = scopeService;

	}

	@Override
	public void setSerializedConfiguration(String properties)
	{
		super.setSerializedConfiguration(properties);
		try
		{
			oauthProperties = new OAuthASProperties(this.properties, pkiManagement,
					getServletUrl(OAUTH_CONSUMER_SERVLET_PATH));
			coordinator.registerAuthzEndpoint(oauthProperties.getValue(OAuthASProperties.ISSUER_URI),
					getServletUrl(OAUTH_CONSUMER_SERVLET_PATH));
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the OAuth 2 AS endpoint's configuration", e);
		}
	}

	@Override
	public ServletContextHandler getServletContextHandler()
	{
		Vaadin2XWebAppContext vaadin2XWebAppContext = new Vaadin2XWebAppContext(properties, genericEndpointProperties, msg, description, authenticationFlows,
				new OAuthCancelHandler(
						new OAuthResponseHandler(applicationContext.getBean(OAuthSessionService.class),
								idpReporterFactory.getForEndpoint(description.getEndpoint()), freemarkerHandler)), sandboxAuthnRouter);
		context = getServletContextHandlerOverridable(vaadin2XWebAppContext);
		return context;
	}

	@Override
	protected ServletContextHandler getServletContextHandlerOverridable(WebAppContext webAppContext)
	{
		if (context != null)
			return context;

		ServletContextHandler servletContextHandler;
		try
		{
			servletContextHandler = getWebAppContext(webAppContext, uiServletPath,
					resourceProvider.getChosenClassPathElement(),
					getWebContentsDir(),
					new ServletContextListeners()
			);
		} catch (Exception e)
		{
			log.error("Creating of web context for endpoint {} failed", description.getEndpoint().getName(), e);
			return context;
		}
		servletContextHandler.setContextPath(description.getEndpoint().getContextAddress());

		Servlet samlParseServlet = new OAuthParseServlet(oauthProperties, getServletUrl(OAUTH_ROUTING_SERVLET_PATH),
				new ErrorHandler(freemarkerHandler), identitiesManagement, attributesManagement, scopeService, serverConfig);
		ServletHolder samlParseHolder = createServletHolder(samlParseServlet);
		servletContextHandler.addServlet(samlParseHolder, OAUTH_CONSUMER_SERVLET_PATH + "/*");

		SessionManagement sessionMan = applicationContext.getBean(SessionManagement.class);
		LoginToHttpSessionBinder sessionBinder = applicationContext.getBean(LoginToHttpSessionBinder.class);
		UnityServerConfiguration config = applicationContext.getBean(UnityServerConfiguration.class);
		RememberMeProcessor remeberMeProcessor = applicationContext.getBean(RememberMeProcessor.class);

		ServletHolder routingServletHolder = createServletHolder(new RoutingServlet(OAUTH_CONSENT_DECIDER_SERVLET_PATH));
		servletContextHandler.addServlet(routingServletHolder, OAUTH_ROUTING_SERVLET_PATH + "/*");

		Servlet oauthConsentDeciderServlet = dispatcherServletFactory.getInstance(getServletUrl(OAUTH_UI_SERVLET_PATH), description);
		ServletHolder oauthConsentDeciderHolder = createServletHolder(oauthConsentDeciderServlet);
		servletContextHandler.addServlet(oauthConsentDeciderHolder, OAUTH_CONSENT_DECIDER_SERVLET_PATH + "/*");

		servletContextHandler.addFilter(new FilterHolder(remoteAuthnResponseProcessingFilter), "/*",
				EnumSet.of(DispatcherType.REQUEST));

		Filter oauthGuardFilter = new OAuthGuardFilter(new ErrorHandler(freemarkerHandler));
		servletContextHandler.addFilter(new FilterHolder(oauthGuardFilter), OAUTH_ROUTING_SERVLET_PATH + "/*",
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		servletContextHandler.addFilter(
				new FilterHolder(new HiddenResourcesFilter(Collections.unmodifiableList(Arrays
						.asList(AUTHENTICATION_PATH, OAUTH_CONSENT_DECIDER_SERVLET_PATH)))),
				"/*", EnumSet.of(DispatcherType.REQUEST));

		authnFilter = new AuthenticationFilter(description.getRealm(), sessionMan, sessionBinder, remeberMeProcessor, new NoSessionFilterImpl());
		servletContextHandler.addFilter(new FilterHolder(authnFilter), "/*",
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		proxyAuthnFilter = new ProxyAuthenticationFilter(authenticationFlows,
				description.getEndpoint().getContextAddress(),
				genericEndpointProperties.getBooleanValue(VaadinEndpointProperties.AUTO_LOGIN), description.getRealm());
		servletContextHandler.addFilter(new FilterHolder(proxyAuthnFilter), AUTHENTICATION_PATH + "/*",
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		contextSetupFilter = new InvocationContextSetupFilter(config, description.getRealm(), null,
				getAuthenticationFlows());
		servletContextHandler.addFilter(new FilterHolder(contextSetupFilter), "/*",
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		return servletContextHandler;
	}
	
	private static class NoSessionFilterImpl implements AuthenticationFilter.NoSessionFilter
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, NoSessionFilterImpl.class);
		
		@Override
		public void doFilter(HttpServletRequest request, HttpServletResponse response) throws EopException, IOException
		{
			AuthenticationPolicy policy = AuthenticationPolicy.getPolicy(request.getSession());
			if (policy.equals(AuthenticationPolicy.REQUIRE_EXISTING_SESSION))
			{
				returnOAuthError(request, response);
				throw new EopException();
			}
		}
		
		private void returnOAuthError(HttpServletRequest request, HttpServletResponse response) throws IOException
		{
			OAuthAuthzContext ctx = OAuthSessionService.getContext(request).get();
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(),
					OIDCError.LOGIN_REQUIRED, ctx.getRequest().getState(),
					ctx.getRequest().impliedResponseMode());
			try
			{
				String redirectURL = oauthResponse.toURI().toString();
				log.trace("Sending OAuth reply via return redirect: " + redirectURL);
				response.sendRedirect(redirectURL);
			} catch (SerializeException | IOException ex)
			{
				throw new IOException("Error: can not serialize error response", ex);
			} 
		}
	}

	@Component
	public static class Factory implements EndpointFactory
	{
		@Autowired
		private ObjectFactory<OAuthAuthzWebEndpoint> factory;

		public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(NAME,
				"OAuth 2 Server - Authorization Grant endpoint", VaadinAuthentication.NAME,
				Collections.singletonMap(OAUTH_CONSUMER_SERVLET_PATH, "OAuth 2 Authorization Grant web endpoint"));

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
