/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import java.util.EnumSet;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.webservice.SAMLLogoutInterface;
import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import io.imunity.vaadin.auth.server.AuthenticationFilter;
import io.imunity.vaadin.auth.server.ProxyAuthenticationFilter;
import io.imunity.vaadin.auth.server.SecureVaadin2XEndpoint;
import io.imunity.vaadin.endpoint.common.InvocationContextSetupFilter;
import io.imunity.vaadin.endpoint.common.RemoteRedirectedAuthnResponseProcessingFilter;
import io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext;
import io.imunity.vaadin.endpoint.common.VaadinEndpointProperties;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.authn.RememberMeProcessor;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.saml.idp.IdpSamlTrustProvider;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfigurationParser;
import pl.edu.icm.unity.saml.idp.SamlIdpStatisticReporter.SamlIdpStatisticReporterFactory;
import pl.edu.icm.unity.saml.idp.TrustedServiceProviders;
import pl.edu.icm.unity.saml.idp.web.filter.ErrorHandler;
import pl.edu.icm.unity.saml.idp.web.filter.IdpConsentDeciderServletFactory;
import pl.edu.icm.unity.saml.idp.web.filter.SamlGuardFilter;
import pl.edu.icm.unity.saml.idp.web.filter.SamlParseServlet;
import pl.edu.icm.unity.saml.idp.ws.SAMLSingleLogoutImpl;
import pl.edu.icm.unity.saml.metadata.MetadataProvider;
import pl.edu.icm.unity.saml.metadata.MetadataProviderFactory;
import pl.edu.icm.unity.saml.metadata.MetadataServlet;
import pl.edu.icm.unity.saml.metadata.cfg.IdpRemoteMetaManager;
import pl.edu.icm.unity.saml.metadata.cfg.MetaToIDPConfigConverter;
import pl.edu.icm.unity.saml.metadata.srv.RemoteMetadataService;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessor;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessor.SamlTrustProvider;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessorFactory;
import pl.edu.icm.unity.saml.slo.SLOReplyInstaller;
import pl.edu.icm.unity.saml.slo.SLOSAMLServlet;
import pl.edu.icm.unity.ws.CXFUtils;
import pl.edu.icm.unity.ws.XmlBeansNsHackOutHandler;
import xmlbeans.org.oasis.saml2.metadata.EndpointType;


@PrototypeComponent
@Primary
public class SamlAuthVaadinEndpoint extends SecureVaadin2XEndpoint
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SamlAuthVaadinEndpoint.class);

	public static final String SAML_CONSUMER_SERVLET_PATH = "/saml2idp-web";
	public static final String SAML_UI_SERVLET_PATH = "/saml2idp-web-ui";
	public static final String SAML_CONSENT_DECIDER_SERVLET_PATH = "/saml2idp-web-consentdecider";
	public static final String SAML_META_SERVLET_PATH = "/metadata";
	public static final String SAML_SLO_ASYNC_SERVLET_PATH = "/SLO-WEB";
	public static final String SAML_SLO_SOAP_SERVLET_PATH = "/SLO-SOAP";


	protected String publicEntryPointPath;
	protected SAMLIdPConfiguration samlConfiguration;
	protected FreemarkerAppHandler freemarkerHandler;
	protected PKIManagement pkiManagement;
	protected ExecutorsService executorsService;
	protected IdpRemoteMetaManager myMetadataManager;
	protected IdpConsentDeciderServletFactory dispatcherServletFactory;
	private final SAMLLogoutProcessorFactory logoutProcessorFactory;
	private final SLOReplyInstaller sloReplyInstaller;
	private final RemoteMetadataService metadataService;
	private final SamlIdpStatisticReporterFactory idpStatisticReporterFactory;

	private final LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement;
	private final URIAccessService uriAccessService;
	private final MessageSource msg;
	private final AttributeTypeSupport aTypeSupport;
	private final SAMLIdPConfigurationParser samlIdPConfigurationParser;


	@Autowired
	public SamlAuthVaadinEndpoint(NetworkServer server,
	                              ApplicationContext applicationContext,
	                              FreemarkerAppHandler freemarkerHandler,
	                              @Qualifier("insecure") PKIManagement pkiManagement,
	                              ExecutorsService executorsService,
	                              IdpConsentDeciderServletFactory dispatcherServletFactory,
	                              SAMLLogoutProcessorFactory logoutProcessorFactory,
	                              SLOReplyInstaller sloReplyInstaller,
	                              MessageSource msg,
	                              AttributeTypeSupport aTypeSupport,
	                              RemoteMetadataService metadataService,
	                              URIAccessService uriAccessService,
	                              AdvertisedAddressProvider advertisedAddrProvider,
	                              RemoteRedirectedAuthnResponseProcessingFilter remoteAuthnResponseProcessingFilter,
	                              SamlIdpStatisticReporterFactory idpStatisticReporterFactory,
	                              LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement,
								  SandboxAuthnRouter sandboxAuthnRouter,
	                              SAMLIdPConfigurationParser samlIdPConfigurationParser)
	{
		this(SAML_CONSUMER_SERVLET_PATH, server, advertisedAddrProvider, applicationContext, freemarkerHandler,
				pkiManagement, executorsService, dispatcherServletFactory, logoutProcessorFactory,
				sloReplyInstaller, msg, aTypeSupport, metadataService, uriAccessService,
				remoteAuthnResponseProcessingFilter, idpStatisticReporterFactory, lastAccessAttributeManagement,
				sandboxAuthnRouter, samlIdPConfigurationParser);
	}

	protected SamlAuthVaadinEndpoint(String publicEntryServletPath,
			NetworkServer server,
			AdvertisedAddressProvider advertisedAddrProvider,
			ApplicationContext applicationContext,
			FreemarkerAppHandler freemarkerHandler,
			PKIManagement pkiManagement,
			ExecutorsService executorsService,
			IdpConsentDeciderServletFactory dispatcherServletFactory,
			SAMLLogoutProcessorFactory logoutProcessorFactory,
			SLOReplyInstaller sloReplyInstaller,
			MessageSource msg,
			AttributeTypeSupport aTypeSupport,
			RemoteMetadataService metadataService,
			URIAccessService uriAccessService,
			RemoteRedirectedAuthnResponseProcessingFilter remoteAuthnResponseProcessingFilter,
			SamlIdpStatisticReporterFactory idpStatisticReporterFactory,
			LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement,
			SandboxAuthnRouter sandboxAuthnRouter,
			SAMLIdPConfigurationParser samlIdPConfigurationParser)
	{
		super(server, advertisedAddrProvider, msg, applicationContext, new SamlResourceProvider(), SAML_CONSENT_DECIDER_SERVLET_PATH,
				remoteAuthnResponseProcessingFilter, sandboxAuthnRouter, SamlVaadin2XServlet.class);
		this.publicEntryPointPath = publicEntryServletPath;
		this.freemarkerHandler = freemarkerHandler;
		this.dispatcherServletFactory = dispatcherServletFactory;
		this.pkiManagement = pkiManagement;
		this.executorsService = executorsService;
		this.logoutProcessorFactory = logoutProcessorFactory;
		this.sloReplyInstaller = sloReplyInstaller;
		this.msg = msg;
		this.aTypeSupport = aTypeSupport;
		this.metadataService = metadataService;
		this.uriAccessService = uriAccessService;
		this.idpStatisticReporterFactory = idpStatisticReporterFactory;
		this.lastAccessAttributeManagement = lastAccessAttributeManagement;
		this.samlIdPConfigurationParser = samlIdPConfigurationParser;
	}
	
	@Override
	public void setSerializedConfiguration(String properties)
	{
		super.setSerializedConfiguration(properties);
		try
		{
			samlConfiguration = samlIdPConfigurationParser.parse(this.properties);
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the SAML Web IdP endpoint's configuration", e);
		}
	}

	@Override
	public void startOverridable()
	{
		myMetadataManager = new IdpRemoteMetaManager(
				samlConfiguration, pkiManagement,
				metadataService, new MetaToIDPConfigConverter(pkiManagement, msg)
		);
		try
		{
			sloReplyInstaller.enable();
		} catch (EngineException e)
		{
			throw new ConfigurationException("Can't initialize the SAML SLO Reply servlet", e);
		}	
	}
	
	@Override
	public void destroyOverridable()
	{
		myMetadataManager.unregisterAll();
	}

	@Override
	public ServletContextHandler getServletContextHandler()
	{
		Vaadin2XWebAppContext vaadin2XWebAppContext = new Vaadin2XWebAppContext(properties, genericEndpointProperties, msg, description, authenticationFlows,
				new SamlAuthnCancelHandler(freemarkerHandler, aTypeSupport, idpStatisticReporterFactory,
						lastAccessAttributeManagement, description.getEndpoint()), sandboxAuthnRouter);
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
			servletContextHandler = getWebAppContext(webAppContext);
		} catch (Exception e)
		{
			log.error("Creating of web context for endpoint {} failed", description.getEndpoint().getName(), e);
			return context;
		}

		String samlPublicEntryPointUrl = getServletUrl(publicEntryPointPath);
		Servlet samlParseServlet = getSamlParseServlet(samlPublicEntryPointUrl, 
				getServletUrl(SAML_CONSENT_DECIDER_SERVLET_PATH));
		ServletHolder samlParseHolder = createServletHolder(samlParseServlet);
		servletContextHandler.addServlet(samlParseHolder, publicEntryPointPath + "/*");

		servletContextHandler.addFilter(new FilterHolder(remoteAuthnResponseProcessingFilter), "/*",
				EnumSet.of(DispatcherType.REQUEST));
		
		Filter samlGuardFilter = new SamlGuardFilter(new ErrorHandler(aTypeSupport, lastAccessAttributeManagement, freemarkerHandler));
		servletContextHandler.addFilter(new FilterHolder(samlGuardFilter), SAML_CONSENT_DECIDER_SERVLET_PATH,
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		Servlet samlConsentDeciderServlet = dispatcherServletFactory.getInstance(
				getServletUrl(SAML_UI_SERVLET_PATH), description.getEndpoint());
		ServletHolder samlConsentDeciderHolder = createServletHolder(samlConsentDeciderServlet);
		servletContextHandler.addServlet(samlConsentDeciderHolder, SAML_CONSENT_DECIDER_SERVLET_PATH + "/*");
		
		String sloAsyncURL = getServletUrl(SAML_SLO_ASYNC_SERVLET_PATH);
		Servlet samlSLOAsyncServlet = getSLOAsyncServlet(sloAsyncURL);
		ServletHolder samlSLOAsyncHolder = createServletHolder(samlSLOAsyncServlet);
		servletContextHandler.addServlet(samlSLOAsyncHolder, SAML_SLO_ASYNC_SERVLET_PATH + "/*");

		String sloSyncURL = getServletUrl(SAML_SLO_SOAP_SERVLET_PATH);

		SessionManagement sessionMan = applicationContext.getBean(SessionManagement.class);
		Servlet samlSLOSyncServlet = getSLOSyncServlet(sloSyncURL);
		ServletHolder samlSLOSyncHolder = createServletHolder(samlSLOSyncServlet);
		servletContextHandler.addServlet(samlSLOSyncHolder, SAML_SLO_SOAP_SERVLET_PATH + "/*");
		
		LoginToHttpSessionBinder sessionBinder = applicationContext.getBean(LoginToHttpSessionBinder.class);
		UnityServerConfiguration config = applicationContext.getBean(UnityServerConfiguration.class);		
		RememberMeProcessor remeberMeProcessor = applicationContext.getBean(RememberMeProcessor.class);
		
		authnFilter = new AuthenticationFilter(description.getRealm(), sessionMan, sessionBinder, remeberMeProcessor);
		servletContextHandler.addFilter(new FilterHolder(authnFilter), "/*",
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		proxyAuthnFilter = new ProxyAuthenticationFilter(authenticationFlows,
				description.getEndpoint().getContextAddress(),
				genericEndpointProperties.getBooleanValue(VaadinEndpointProperties.AUTO_LOGIN),
				description.getRealm());
		servletContextHandler.addFilter(new FilterHolder(proxyAuthnFilter), SAML_UI_SERVLET_PATH + "/*",
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		contextSetupFilter = new InvocationContextSetupFilter(config, description.getRealm(),
				null, getAuthenticationFlows());
		servletContextHandler.addFilter(new FilterHolder(contextSetupFilter), "/*",
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		if (samlConfiguration.publishMetadata)
		{
			Servlet metadataServlet = getMetadataServlet(samlPublicEntryPointUrl, sloAsyncURL, sloSyncURL);
			servletContextHandler.addServlet(createServletHolder(metadataServlet), SAML_META_SERVLET_PATH + "/*");
		}
		return servletContextHandler;
	}
	protected Servlet getSamlParseServlet(String endpointURL, String dispatcherUrl)
	{
		return new SamlParseServlet(myMetadataManager, 
				endpointURL, dispatcherUrl, new ErrorHandler(aTypeSupport, lastAccessAttributeManagement, freemarkerHandler));
	}

	protected Servlet getMetadataServlet(String samlEndpointURL, String sloEndpointURL, String sloSoapEndpointURL)
	{
		EndpointType ssoPost = EndpointType.Factory.newInstance();
		ssoPost.setLocation(samlEndpointURL);
		ssoPost.setBinding(SAMLConstants.BINDING_HTTP_POST);
		EndpointType ssoRedirect = EndpointType.Factory.newInstance();
		ssoRedirect.setLocation(samlEndpointURL);
		ssoRedirect.setBinding(SAMLConstants.BINDING_HTTP_REDIRECT);
		EndpointType[] authnEndpoints = new EndpointType[] {ssoPost, ssoRedirect};
		
		EndpointType sloPost = EndpointType.Factory.newInstance();
		sloPost.setLocation(sloEndpointURL);
		sloPost.setBinding(SAMLConstants.BINDING_HTTP_POST);
		sloPost.setResponseLocation(sloReplyInstaller.getServletURL());
		EndpointType sloRedirect = EndpointType.Factory.newInstance();
		sloRedirect.setLocation(sloEndpointURL);
		sloRedirect.setResponseLocation(sloReplyInstaller.getServletURL());
		sloRedirect.setBinding(SAMLConstants.BINDING_HTTP_REDIRECT);
		EndpointType sloSoap = EndpointType.Factory.newInstance();
		sloSoap.setLocation(sloSoapEndpointURL + "/SingleLogoutService");
		sloSoap.setBinding(SAMLConstants.BINDING_SOAP);
		EndpointType[] sloEndpoints = new EndpointType[] {sloPost, sloRedirect, sloSoap};

		MetadataProvider provider = MetadataProviderFactory.newIdpInstance(myMetadataManager.getSAMLIdPConfiguration(), uriAccessService,
				executorsService, authnEndpoints, null, sloEndpoints,
				description.getEndpoint().getConfiguration().getDisplayedName(), msg);
		return new MetadataServlet(provider);
	}
	
	protected Servlet getSLOAsyncServlet(String endpointURL)
	{
		SAMLLogoutProcessor logoutProcessor = createLogoutProcessor(endpointURL);
		return new SLOSAMLServlet(logoutProcessor);
	}
	
	protected Servlet getSLOSyncServlet(String endpointURL)
	{
		SAMLLogoutProcessor logoutProcessor = createLogoutProcessor(endpointURL + "/SingleLogoutService");
		SAMLSingleLogoutImpl webService = new SAMLSingleLogoutImpl(logoutProcessor);
		
		CXFNonSpringServlet cxfServlet = new CXFNonSpringServlet();
		Bus bus = BusFactory.newInstance().createBus();
		cxfServlet.setBus(bus);
		Endpoint cxfEndpoint = CXFUtils.deployWebservice(bus, SAMLLogoutInterface.class, webService);
		cxfEndpoint.getOutInterceptors().add(new XmlBeansNsHackOutHandler());
		
		return cxfServlet;
	}
	
	public TrustedServiceProviders getSpsConfiguration()
	{
		return myMetadataManager.getTrustedSps();
	}
	
	private SAMLLogoutProcessor createLogoutProcessor(String endpointURL)
	{
		SamlTrustProvider trustProvider = new IdpSamlTrustProvider(myMetadataManager);
		SAMLIdPConfiguration configuration = myMetadataManager.getSAMLIdPConfiguration();
		ResolvedEndpoint endpointDescription = getEndpointDescription();
		return logoutProcessorFactory.getInstance(
				configuration.idTypeMapper,
				endpointURL,
				configuration.requestValidityPeriod,
				configuration.issuerURI,
				configuration.getSamlIssuerCredential(),
				trustProvider, 
				endpointDescription.getRealm().getName());
	}

}
