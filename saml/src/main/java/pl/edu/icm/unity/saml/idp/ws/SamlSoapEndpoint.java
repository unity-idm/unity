/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.ws;

import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.Servlet;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.webservice.SAMLAuthnInterface;
import eu.unicore.samly2.webservice.SAMLLogoutInterface;
import eu.unicore.samly2.webservice.SAMLQueryInterface;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.saml.idp.IdpSamlTrustProvider;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.SamlIdpStatisticReporter.SamlIdpStatisticReporterFactory;
import pl.edu.icm.unity.saml.metadata.MetadataProvider;
import pl.edu.icm.unity.saml.metadata.MetadataProviderFactory;
import pl.edu.icm.unity.saml.metadata.MetadataServlet;
import pl.edu.icm.unity.saml.metadata.cfg.MetaToIDPConfigConverter;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.saml.metadata.srv.RemoteMetadataService;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessor;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessor.SamlTrustProvider;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessorFactory;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.ws.CXFEndpoint;
import pl.edu.icm.unity.ws.authn.WebServiceAuthentication;
import xmlbeans.org.oasis.saml2.metadata.EndpointType;

/**
 * Endpoint exposing SAML SOAP binding.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
@Primary
public class SamlSoapEndpoint extends CXFEndpoint
{
	public static final String NAME = "SAMLSoapIdP";
	public static final String SERVLET_PATH = "/saml2idp-soap";
	public static final String METADATA_SERVLET_PATH = "/metadata";
	
	protected SamlIdpProperties samlProperties;
	protected PreferencesManagement preferencesMan;
	protected IdPEngine idpEngine;
	protected PKIManagement pkiManagement;
	protected ExecutorsService executorsService;
	protected RemoteMetaManager myMetadataManager;
	private SAMLLogoutProcessorFactory logoutProcessorFactory;
	protected AttributeTypeSupport aTypeSupport;
	private RemoteMetadataService metadataService;
	private URIAccessService uriAccessService;
	protected final SamlIdpStatisticReporterFactory idpStatisticReporterFactory;
	
	@Autowired
	public SamlSoapEndpoint(MessageSource msg,
			NetworkServer server,
			IdPEngine idpEngine,
			PreferencesManagement preferencesMan,
			@Qualifier("insecure") PKIManagement pkiManagement,
			ExecutorsService executorsService,
			SessionManagement sessionMan,
			SAMLLogoutProcessorFactory logoutProcessorFactory,
			AuthenticationProcessor authnProcessor,
			AttributeTypeSupport aTypeSupport,
			RemoteMetadataService metadataService,
			URIAccessService uriAccessService,
			AdvertisedAddressProvider advertisedAddrProvider,
			EntityManagement entityMan, 
			SamlIdpStatisticReporterFactory idpStatisticReporterFactory)
	{
		super(msg, sessionMan, authnProcessor, server, advertisedAddrProvider, SERVLET_PATH, entityMan);
		this.idpEngine = idpEngine;
		this.preferencesMan = preferencesMan;
		this.pkiManagement = pkiManagement;
		this.executorsService = executorsService;
		this.logoutProcessorFactory = logoutProcessorFactory;
		this.aTypeSupport = aTypeSupport;
		this.metadataService = metadataService;
		this.uriAccessService = uriAccessService;
		this.idpStatisticReporterFactory = idpStatisticReporterFactory;
	}

	@Override
	public void setSerializedConfiguration(String config)
	{
		super.setSerializedConfiguration(config);
		try
		{
			samlProperties = new SamlIdpProperties(properties, pkiManagement);
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the SAML SOAP" +
					" IdP endpoint's configuration", e);
		}
	}

	@Override
	public void startOverridable()
	{
		myMetadataManager = new RemoteMetaManager(samlProperties, 
				pkiManagement, 
				new MetaToIDPConfigConverter(pkiManagement, msg), 
				metadataService, SamlIdpProperties.SPMETA_PREFIX);
	}
	
	@Override
	public void destroyOverridable()
	{
		myMetadataManager.unregisterAll();
	}

	@Override
	public ServletContextHandler getServletContextHandler()
	{
		ServletContextHandler context = super.getServletContextHandler();
		
		String endpointURL = getServletUrl(servletPath);
		Servlet metadataServlet = getMetadataServlet(endpointURL);
		
		if (samlProperties.getBooleanValue(SamlIdpProperties.PUBLISH_METADATA))
		{
			ServletHolder holder = new ServletHolder(metadataServlet);
			context.addServlet(holder, METADATA_SERVLET_PATH + "/*");
		}
		return context;
	}
	
	@Override
	protected void configureServices()
	{
		String endpointURL = getServletUrl(servletPath);
		SamlIdpProperties virtualConf = (SamlIdpProperties) myMetadataManager.getVirtualConfiguration();
		SAMLAssertionQueryImpl assertionQueryImpl = new SAMLAssertionQueryImpl(aTypeSupport, virtualConf, 
				endpointURL, idpEngine, preferencesMan);
		addWebservice(SAMLQueryInterface.class, assertionQueryImpl);
		SAMLAuthnImpl authnImpl = new SAMLAuthnImpl(aTypeSupport, virtualConf, endpointURL, 
				idpEngine, preferencesMan, idpStatisticReporterFactory.getForEndpoint(description.getEndpoint()));
		addWebservice(SAMLAuthnInterface.class, authnImpl);
		
		configureSLOService(virtualConf, endpointURL);
	}
	
	protected void configureSLOService(SamlIdpProperties virtualConf, String endpointURL)
	{
		SamlTrustProvider trustProvider = new IdpSamlTrustProvider(myMetadataManager);
		SAMLLogoutProcessor logoutProcessor = logoutProcessorFactory.getInstance(virtualConf.getIdTypeMapper(), 
				endpointURL + "/SingleLogoutService", 
				virtualConf.getLongValue(SamlIdpProperties.SAML_REQUEST_VALIDITY), 
				virtualConf.getValue(SamlIdpProperties.ISSUER_URI), 
				virtualConf.getSamlIssuerCredential(), 
				trustProvider, 
				getEndpointDescription().getRealm().getName());
		SAMLSingleLogoutImpl logoutImpl = new SAMLSingleLogoutImpl(logoutProcessor);
		addWebservice(SAMLLogoutInterface.class, logoutImpl);
	}
	
	protected Servlet getMetadataServlet(String samlEndpointURL)
	{
		EndpointType ssoSoap = EndpointType.Factory.newInstance();
		ssoSoap.setLocation(samlEndpointURL + "/AuthenticationService");
		ssoSoap.setBinding(SAMLConstants.BINDING_SOAP);
		EndpointType[] ssoEndpoints = new EndpointType[] {ssoSoap};

		EndpointType attributeSoap = EndpointType.Factory.newInstance();
		attributeSoap.setLocation(samlEndpointURL + "/AssertionQueryService");
		attributeSoap.setBinding(SAMLConstants.BINDING_SOAP);
		EndpointType[] attributeQueryEndpoints = new EndpointType[] {attributeSoap};

		EndpointType sloSoap = EndpointType.Factory.newInstance();
		sloSoap.setLocation(samlEndpointURL + "/SingleLogoutService");
		sloSoap.setBinding(SAMLConstants.BINDING_SOAP);
		EndpointType[] sloEndpoints = new EndpointType[] {sloSoap};
		
		MetadataProvider provider = MetadataProviderFactory.newIdpInstance(samlProperties, uriAccessService, 
				executorsService, ssoEndpoints, attributeQueryEndpoints, sloEndpoints);
		return new MetadataServlet(provider);
	}
	
	
	@Component
	public static class Factory implements EndpointFactory
	{
		@Autowired
		private ObjectFactory<SamlSoapEndpoint> factory;
		
		public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(NAME,
				"SAML 2 identity provider web endpoint", WebServiceAuthentication.NAME,
				Stream.of(new AbstractMap.SimpleEntry<>(SERVLET_PATH,
						"SAML 2 identity provider web endpoint"),
						new AbstractMap.SimpleEntry<>(METADATA_SERVLET_PATH,
								"Metadata of the SAML 2 identity provider web endpoint"))
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
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




