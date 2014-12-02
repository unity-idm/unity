/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import java.util.EnumSet;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.context.ApplicationContext;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.web.filter.ErrorHandler;
import pl.edu.icm.unity.saml.idp.web.filter.SamlGuardFilter;
import pl.edu.icm.unity.saml.idp.web.filter.SamlParseServlet;
import pl.edu.icm.unity.saml.metadata.MetadataProvider;
import pl.edu.icm.unity.saml.metadata.MetadataProviderFactory;
import pl.edu.icm.unity.saml.metadata.MetadataServlet;
import pl.edu.icm.unity.saml.metadata.cfg.MetaDownloadManager;
import pl.edu.icm.unity.saml.metadata.cfg.MetaToIDPConfigConverter;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessor;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessor.SamlTrustProvider;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessorFactory;
import pl.edu.icm.unity.saml.slo.SLOReplyInstaller;
import pl.edu.icm.unity.saml.slo.SLOSAMLServlet;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.authn.LoginToHttpSessionBinder;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityVaadinServlet;
import pl.edu.icm.unity.webui.VaadinEndpoint;
import pl.edu.icm.unity.webui.authn.AuthenticationFilter;
import pl.edu.icm.unity.webui.authn.AuthenticationUI;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import xmlbeans.org.oasis.saml2.metadata.EndpointType;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * Extends a simple {@link VaadinEndpoint} with configuration of SAML authn filter. Also SAML configuration
 * is parsed here.
 * 
 * @author K. Benedyczak
 */
public class SamlAuthVaadinEndpoint extends VaadinEndpoint
{
	protected SamlIdpProperties samlProperties;
	protected FreemarkerHandler freemarkerHandler;
	protected PKIManagement pkiManagement;
	protected ExecutorsService executorsService;
	protected String samlConsumerPath;
	protected String samlSLOPath;
	protected String samlMetadataPath;
	protected RemoteMetaManager myMetadataManager;
	private Map<String, RemoteMetaManager> remoteMetadataManagers;
	private MetaDownloadManager downloadManager;
	private UnityServerConfiguration mainConfig;
	private SAMLLogoutProcessorFactory logoutProcessorFactory;
	private SLOReplyInstaller sloReplyInstaller;
	
	public SamlAuthVaadinEndpoint(EndpointTypeDescription type,
			ApplicationContext applicationContext, FreemarkerHandler freemarkerHandler,
			Class<?> uiClass, String samlUiServletPath, PKIManagement pkiManagement,
			ExecutorsService executorsService, UnityServerConfiguration mainConfig,
			Map<String, RemoteMetaManager> remoteMetadataManagers,
			MetaDownloadManager downloadManager, String samlConsumerPath,
			String samlMetadataPath, String samlSLOPath, 
			SAMLLogoutProcessorFactory logoutProcessorFactory, SLOReplyInstaller sloReplyInstaller)
	{
		super(type, applicationContext, uiClass.getSimpleName(), samlUiServletPath);
		this.freemarkerHandler = freemarkerHandler;
		this.pkiManagement = pkiManagement;
		this.executorsService = executorsService;
		this.samlConsumerPath = samlConsumerPath;
		this.samlMetadataPath = samlMetadataPath;
		this.samlSLOPath = samlSLOPath;
		this.remoteMetadataManagers = remoteMetadataManagers;
		this.downloadManager = downloadManager;
		this.mainConfig = mainConfig;
		this.logoutProcessorFactory = logoutProcessorFactory;
		this.sloReplyInstaller = sloReplyInstaller;
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
		
		String id = getEndpointDescription().getId();
		if (!remoteMetadataManagers.containsKey(id))
		{
			myMetadataManager = new RemoteMetaManager(samlProperties, 
					mainConfig, executorsService, pkiManagement, 
					new MetaToIDPConfigConverter(pkiManagement), downloadManager, 
					SamlIdpProperties.SPMETA_PREFIX);
			remoteMetadataManagers.put(id, myMetadataManager);
			myMetadataManager.start();
		} else
		{
			myMetadataManager = remoteMetadataManagers.get(id);
			myMetadataManager.setBaseConfiguration(samlProperties);
		}
		
		try
		{
			sloReplyInstaller.enable();
		} catch (EngineException e)
		{
			throw new ConfigurationException("Can't initialize the SAML SLO Reply servlet", e);
		}	
	}

	@Override
	public ServletContextHandler getServletContextHandler()
	{	
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(description.getContextAddress());

		String endpointURL = getServletUrl(samlConsumerPath);
		String uiURL = getServletUrl(servletPath);
		Filter samlGuardFilter = new SamlGuardFilter(servletPath, new ErrorHandler(freemarkerHandler));
		context.addFilter(new FilterHolder(samlGuardFilter), servletPath + "/*", 
				EnumSet.of(DispatcherType.REQUEST));
		
		Servlet samlParseServlet = getSamlParseServlet(endpointURL, uiURL);
		ServletHolder samlParseHolder = createServletHolder(samlParseServlet, true);
		context.addServlet(samlParseHolder, samlConsumerPath + "/*");
		
		String sloURL = getServletUrl(samlSLOPath);
		Servlet samlSLOServlet = getSLORequestServlet(sloURL);
		ServletHolder samlSLOHolder = createServletHolder(samlSLOServlet, true);
		context.addServlet(samlSLOHolder, samlSLOPath + "/*");
		
		SessionManagement sessionMan = applicationContext.getBean(SessionManagement.class);
		LoginToHttpSessionBinder sessionBinder = applicationContext.getBean(LoginToHttpSessionBinder.class);
		
		AuthenticationFilter authnFilter = new AuthenticationFilter(servletPath, 
				AUTHENTICATION_PATH, description.getRealm(), sessionMan, sessionBinder);
		context.addFilter(new FilterHolder(authnFilter), "/*", 
				EnumSet.of(DispatcherType.REQUEST));
		
		EndpointRegistrationConfiguration registrationConfiguration = getRegistrationConfiguration();
		UnityVaadinServlet authenticationServlet = new UnityVaadinServlet(applicationContext, 
				AuthenticationUI.class.getSimpleName(), description, authenticators,
				registrationConfiguration);
		
		CancelHandler cancelHandler = new SamlAuthnCancelHandler(freemarkerHandler);
		authenticationServlet.setCancelHandler(cancelHandler);
		
		ServletHolder authnServletHolder = createVaadinServletHolder(authenticationServlet, true); 
		context.addServlet(authnServletHolder, AUTHENTICATION_PATH+"/*");
		context.addServlet(authnServletHolder, VAADIN_RESOURCES);
		
		UnityVaadinServlet theServlet = new UnityVaadinServlet(applicationContext, uiBeanName,
				description, authenticators, registrationConfiguration);
		context.addServlet(createVaadinServletHolder(theServlet, false), servletPath + "/*");
		
		if (samlProperties.getBooleanValue(SamlIdpProperties.PUBLISH_METADATA))
		{
			Servlet metadataServlet = getMetadataServlet(endpointURL, sloURL);
			context.addServlet(createServletHolder(metadataServlet, true), samlMetadataPath + "/*");
		}
		return context;
	}
	
	protected Servlet getSamlParseServlet(String endpointURL, String uiUrl)
	{
		return new SamlParseServlet(myMetadataManager, 
				endpointURL, uiUrl, new ErrorHandler(freemarkerHandler));
	}
	
	protected Servlet getMetadataServlet(String samlEndpointURL, String sloEndpointURL)
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
		EndpointType[] sloEndpoints = new EndpointType[] {sloPost, sloRedirect};
		
		MetadataProvider provider = MetadataProviderFactory.newIdpInstance(samlProperties, 
				executorsService, authnEndpoints, null, sloEndpoints);
		return new MetadataServlet(provider);
	}
	
	protected Servlet getSLORequestServlet(String endpointURL)
	{
		SamlTrustProvider trustProvider = new SamlTrustProvider()
		{
			@Override
			public SamlTrustChecker getTrustChecker()
			{
				SamlIdpProperties virtualConf = (SamlIdpProperties) 
						myMetadataManager.getVirtualConfiguration();
				return virtualConf.getAuthnTrustChecker();
			}
		};
		SamlIdpProperties virtualConf = (SamlIdpProperties) myMetadataManager.getVirtualConfiguration();
		SAMLLogoutProcessor logoutProcessor = logoutProcessorFactory.getInstance(virtualConf.getIdTypeMapper(), 
				endpointURL, 
				virtualConf.getLongValue(SamlIdpProperties.SAML_REQUEST_VALIDITY) * 1000, 
				virtualConf.getValue(SamlIdpProperties.ISSUER_URI), 
				virtualConf.getSamlIssuerCredential(), 
				trustProvider, 
				getEndpointDescription().getRealm().getName());
		
		return new SLOSAMLServlet(logoutProcessor);
	}
}
