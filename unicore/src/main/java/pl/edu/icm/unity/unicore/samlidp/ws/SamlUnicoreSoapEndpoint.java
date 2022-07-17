/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.ws;

import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import eu.unicore.samly2.webservice.SAMLAuthnInterface;
import eu.unicore.samly2.webservice.SAMLQueryInterface;
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
import pl.edu.icm.unity.saml.idp.LastAccessAttributeManagement;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.idp.SamlIdpStatisticReporter.SamlIdpStatisticReporterFactory;
import pl.edu.icm.unity.saml.idp.ws.SAMLAssertionQueryImpl;
import pl.edu.icm.unity.saml.idp.ws.SamlSoapEndpoint;
import pl.edu.icm.unity.saml.metadata.srv.RemoteMetadataService;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessorFactory;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.ws.authn.WebServiceAuthentication;

/**
 * Endpoint exposing SAML SOAP binding. This version extends the {@link SamlSoapEndpoint}
 * by exposing a modified implementation of the {@link SAMLAuthnInterface}. The
 * {@link SAMLETDAuthnImpl} is used, which also returns a bootstrap ETD assertion.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class SamlUnicoreSoapEndpoint extends SamlSoapEndpoint
{
	public static final String NAME = "SAMLUnicoreSoapIdP";
	
	public static final String SERVLET_PATH = "/saml2unicoreidp-soap";
	
	@Autowired
	public SamlUnicoreSoapEndpoint(MessageSource msg,
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
			SamlIdpStatisticReporterFactory idpStatisticReporterFactory,
			LastAccessAttributeManagement lastAccessAttributeManagement)
	{
		super(msg, server, idpEngine, preferencesMan, pkiManagement, executorsService, sessionMan,
				logoutProcessorFactory, authnProcessor, aTypeSupport, metadataService, uriAccessService,
				advertisedAddrProvider, entityMan, idpStatisticReporterFactory, lastAccessAttributeManagement);
		this.servletPath = SERVLET_PATH;
	}


	@Override
	protected void configureServices()
	{
		String endpointURL = getServletUrl(servletPath);
		SamlIdpProperties virtualConf = (SamlIdpProperties) myMetadataManager.getVirtualConfiguration();
		SAMLAssertionQueryImpl assertionQueryImpl = new SAMLAssertionQueryImpl(aTypeSupport, virtualConf, 
				endpointURL, idpEngine, preferencesMan);
		addWebservice(SAMLQueryInterface.class, assertionQueryImpl);
		SAMLETDAuthnImpl authnImpl = new SAMLETDAuthnImpl(aTypeSupport, virtualConf, endpointURL, 
				idpEngine, preferencesMan, idpStatisticReporterFactory.getForEndpoint(description.getEndpoint()), lastAccessAttributeManagement);
		addWebservice(SAMLAuthnInterface.class, authnImpl);
	}
	
	@Component
	public static class Factory implements EndpointFactory
	{
		@Autowired
		private ObjectFactory<SamlUnicoreSoapEndpoint> factory;
		
		public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(NAME,
				"SAML 2 identity provider web endpoint", WebServiceAuthentication.NAME,
				Stream.of(new AbstractMap.SimpleEntry<>(SERVLET_PATH,
						"SAML 2 UNICORE identity provider web endpoint"),
						new AbstractMap.SimpleEntry<>(SamlSoapEndpoint.METADATA_SERVLET_PATH,
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




