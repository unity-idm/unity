/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.webservice.SAMLLogoutInterface;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.LoginToHttpSessionBinder;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.engine.api.utils.HiddenResourcesFilter;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.engine.api.utils.RoutingServlet;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.idp.IdpSamlTrustProvider;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.web.filter.ErrorHandler;
import pl.edu.icm.unity.saml.idp.web.filter.IdpConsentDeciderServletFactory;
import pl.edu.icm.unity.saml.idp.web.filter.SamlGuardFilter;
import pl.edu.icm.unity.saml.idp.web.filter.SamlParseServlet;
import pl.edu.icm.unity.saml.idp.ws.SAMLSingleLogoutImpl;
import pl.edu.icm.unity.saml.metadata.MetadataProvider;
import pl.edu.icm.unity.saml.metadata.MetadataProviderFactory;
import pl.edu.icm.unity.saml.metadata.MetadataServlet;
import pl.edu.icm.unity.saml.metadata.cfg.MetaToIDPConfigConverter;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.saml.metadata.srv.RemoteMetadataService;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessor;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessor.SamlTrustProvider;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessorFactory;
import pl.edu.icm.unity.saml.slo.SLOReplyInstaller;
import pl.edu.icm.unity.saml.slo.SLOSAMLServlet;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityVaadinServlet;
import pl.edu.icm.unity.webui.VaadinEndpoint;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.AuthenticationFilter;
import pl.edu.icm.unity.webui.authn.AuthenticationUI;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.authn.InvocationContextSetupFilter;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationFilter;
import pl.edu.icm.unity.webui.authn.RememberMeProcessor;
import pl.edu.icm.unity.ws.CXFUtils;
import pl.edu.icm.unity.ws.XmlBeansNsHackOutHandler;
import xmlbeans.org.oasis.saml2.metadata.EndpointType;

/**
 * Extends a simple {@link VaadinEndpoint} with configuration of SAML authn filter. Also SAML configuration
 * is parsed here.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
@Primary
public class SamlAuthVaadinEndpoint extends VaadinEndpoint
{
	public static final String SAML_ENTRY_SERVLET_PATH = "/saml2idp-web-entry";
	public static final String SAML_CONSUMER_SERVLET_PATH = "/saml2idp-web";
	public static final String SAML_UI_SERVLET_PATH = "/saml2idp-web-ui";
	public static final String SAML_CONSENT_DECIDER_SERVLET_PATH = "/saml2idp-web-consentdecider";
	public static final String SAML_META_SERVLET_PATH = "/metadata";
	public static final String SAML_SLO_ASYNC_SERVLET_PATH = "/SLO-WEB";
	public static final String SAML_SLO_SOAP_SERVLET_PATH = "/SLO-SOAP";
	
	protected String publicEntryPointPath;
	protected SamlIdpProperties samlProperties;
	protected FreemarkerAppHandler freemarkerHandler;
	protected PKIManagement pkiManagement;
	protected ExecutorsService executorsService;
	protected RemoteMetaManager myMetadataManager;
	protected IdpConsentDeciderServletFactory dispatcherServletFactory;
	private SAMLLogoutProcessorFactory logoutProcessorFactory;
	private SLOReplyInstaller sloReplyInstaller;
	private UnityMessageSource msg;
	protected AttributeTypeSupport aTypeSupport;
	private RemoteMetadataService metadataService;
	
	@Autowired
	public SamlAuthVaadinEndpoint(NetworkServer server,
			ApplicationContext applicationContext, FreemarkerAppHandler freemarkerHandler,
			@Qualifier("insecure") PKIManagement pkiManagement,
			ExecutorsService executorsService, 
			IdpConsentDeciderServletFactory dispatcherServletFactory,
			SAMLLogoutProcessorFactory logoutProcessorFactory, SLOReplyInstaller sloReplyInstaller,
			UnityMessageSource msg, AttributeTypeSupport aTypeSupport,
			RemoteMetadataService metadataService)
	{
		this(SAML_CONSUMER_SERVLET_PATH, server, applicationContext, freemarkerHandler, SamlIdPWebUI.class, 
				pkiManagement, executorsService, dispatcherServletFactory, 
				logoutProcessorFactory, sloReplyInstaller, msg, 
				aTypeSupport, metadataService);
	}
	
	protected SamlAuthVaadinEndpoint(String publicEntryServletPath, NetworkServer server,
			ApplicationContext applicationContext, FreemarkerAppHandler freemarkerHandler,
			Class<?> uiClass, PKIManagement pkiManagement,
			ExecutorsService executorsService, 
			IdpConsentDeciderServletFactory dispatcherServletFactory,
			SAMLLogoutProcessorFactory logoutProcessorFactory, SLOReplyInstaller sloReplyInstaller,
			UnityMessageSource msg, AttributeTypeSupport aTypeSupport,
			RemoteMetadataService metadataService)
	{
		super(server, msg, applicationContext, uiClass.getSimpleName(), SAML_UI_SERVLET_PATH);
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
	}
	
	@Override
	public void setSerializedConfiguration(String properties)
	{
		super.setSerializedConfiguration(properties);
		try
		{
			samlProperties = new SamlIdpProperties(this.properties, pkiManagement);
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the SAML Web IdP endpoint's configuration", e);
		}
	}

	@Override
	public void startOverridable()
	{
		myMetadataManager = new RemoteMetaManager(samlProperties, pkiManagement,
					new MetaToIDPConfigConverter(pkiManagement, msg), 
					metadataService, SamlIdpProperties.SPMETA_PREFIX);
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
	protected ServletContextHandler getServletContextHandlerOverridable()
	{	
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(description.getEndpoint().getContextAddress());

		String samlPublicEntryPointUrl = getServletUrl(publicEntryPointPath);
		Servlet samlParseServlet = getSamlParseServlet(samlPublicEntryPointUrl, 
				getServletUrl(SAML_ENTRY_SERVLET_PATH));
		ServletHolder samlParseHolder = createServletHolder(samlParseServlet, true);
		context.addServlet(samlParseHolder, publicEntryPointPath + "/*");

		Filter samlGuardFilter = new SamlGuardFilter(new ErrorHandler(aTypeSupport, freemarkerHandler));
		context.addFilter(new FilterHolder(samlGuardFilter), SAML_ENTRY_SERVLET_PATH, 
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		ServletHolder routingServletHolder = createServletHolder(
				new RoutingServlet(SAML_CONSENT_DECIDER_SERVLET_PATH), true);
		context.addServlet(routingServletHolder, SAML_ENTRY_SERVLET_PATH + "/*");
		
		Servlet samlConsentDeciderServlet = dispatcherServletFactory.getInstance(
				SAML_UI_SERVLET_PATH, AUTHENTICATION_PATH);
		ServletHolder samlConsentDeciderHolder = createServletHolder(samlConsentDeciderServlet, true);
		context.addServlet(samlConsentDeciderHolder, SAML_CONSENT_DECIDER_SERVLET_PATH + "/*");
		
		String sloAsyncURL = getServletUrl(SAML_SLO_ASYNC_SERVLET_PATH);
		Servlet samlSLOAsyncServlet = getSLOAsyncServlet(sloAsyncURL);
		ServletHolder samlSLOAsyncHolder = createServletHolder(samlSLOAsyncServlet, true);
		context.addServlet(samlSLOAsyncHolder, SAML_SLO_ASYNC_SERVLET_PATH + "/*");

		String sloSyncURL = getServletUrl(SAML_SLO_SOAP_SERVLET_PATH);
		Servlet samlSLOSyncServlet = getSLOSyncServlet(sloSyncURL);
		ServletHolder samlSLOSyncHolder = createServletHolder(samlSLOSyncServlet, true);
		context.addServlet(samlSLOSyncHolder, SAML_SLO_SOAP_SERVLET_PATH + "/*");
		
		SessionManagement sessionMan = applicationContext.getBean(SessionManagement.class);
		LoginToHttpSessionBinder sessionBinder = applicationContext.getBean(LoginToHttpSessionBinder.class);
		UnityServerConfiguration config = applicationContext.getBean(UnityServerConfiguration.class);		
		RememberMeProcessor remeberMeProcessor = applicationContext.getBean(RememberMeProcessor.class);
		
		context.addFilter(new FilterHolder(new HiddenResourcesFilter(
				Collections.unmodifiableList(Arrays.asList(AUTHENTICATION_PATH, 
						SAML_CONSENT_DECIDER_SERVLET_PATH, SAML_UI_SERVLET_PATH)))), 
				"/*", EnumSet.of(DispatcherType.REQUEST));
		authnFilter = new AuthenticationFilter(
				Arrays.asList(SAML_ENTRY_SERVLET_PATH), 
				AUTHENTICATION_PATH, description.getRealm(), sessionMan, sessionBinder, remeberMeProcessor);
		context.addFilter(new FilterHolder(authnFilter), "/*", 
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		proxyAuthnFilter = new ProxyAuthenticationFilter(authenticationFlows, 
				description.getEndpoint().getContextAddress(),
				genericEndpointProperties.getBooleanValue(VaadinEndpointProperties.AUTO_LOGIN));
		context.addFilter(new FilterHolder(proxyAuthnFilter), AUTHENTICATION_PATH + "/*", 
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));

		contextSetupFilter = new InvocationContextSetupFilter(config, description.getRealm(),
				null, getAuthenticationFlows());
		context.addFilter(new FilterHolder(contextSetupFilter), "/*", 
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD));
		
		EndpointRegistrationConfiguration registrationConfiguration = genericEndpointProperties.getRegistrationConfiguration();
		authenticationServlet = new UnityVaadinServlet(applicationContext, 
				AuthenticationUI.class.getSimpleName(), description, authenticationFlows,
				registrationConfiguration, properties, 
				getBootstrapHandler4Authn(SAML_ENTRY_SERVLET_PATH));
		
		CancelHandler cancelHandler = new SamlAuthnCancelHandler(freemarkerHandler, aTypeSupport);
		authenticationServlet.setCancelHandler(cancelHandler);
		
		ServletHolder authnServletHolder = createVaadinServletHolder(authenticationServlet, true);
		context.addServlet(authnServletHolder, AUTHENTICATION_PATH+"/*");
		context.addServlet(authnServletHolder, VAADIN_RESOURCES);
		
		theServlet = new UnityVaadinServlet(applicationContext, uiBeanName,
				description, authenticationFlows, registrationConfiguration, properties, 
				getBootstrapHandler(SAML_ENTRY_SERVLET_PATH));
		context.addServlet(createVaadinServletHolder(theServlet, false), uiServletPath + "/*");
		
		if (samlProperties.getBooleanValue(SamlIdpProperties.PUBLISH_METADATA))
		{
			Servlet metadataServlet = getMetadataServlet(samlPublicEntryPointUrl, sloAsyncURL, sloSyncURL);
			context.addServlet(createServletHolder(metadataServlet, true), SAML_META_SERVLET_PATH + "/*");
		}
		return context;
	}
	
	protected Servlet getSamlParseServlet(String endpointURL, String dispatcherUrl)
	{
		return new SamlParseServlet(myMetadataManager, 
				endpointURL, dispatcherUrl, new ErrorHandler(aTypeSupport, freemarkerHandler));
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
		sloSoap.setLocation(sloSoapEndpointURL);
		sloSoap.setBinding(SAMLConstants.BINDING_SOAP);
		EndpointType[] sloEndpoints = new EndpointType[] {sloPost, sloRedirect, sloSoap};
		
		MetadataProvider provider = MetadataProviderFactory.newIdpInstance(samlProperties, 
				executorsService, authnEndpoints, null, sloEndpoints);
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
	
	private SAMLLogoutProcessor createLogoutProcessor(String endpointURL)
	{
		SamlTrustProvider trustProvider = new IdpSamlTrustProvider(myMetadataManager);
		SamlIdpProperties virtualConf = (SamlIdpProperties) myMetadataManager.getVirtualConfiguration();
		return logoutProcessorFactory.getInstance(
				virtualConf.getIdTypeMapper(), 
				endpointURL, 
				virtualConf.getLongValue(SamlIdpProperties.SAML_REQUEST_VALIDITY) * 1000, 
				virtualConf.getValue(SamlIdpProperties.ISSUER_URI), 
				virtualConf.getSamlIssuerCredential(), 
				trustProvider, 
				getEndpointDescription().getRealm().getName());
	}
}
