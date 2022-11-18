/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.web;

import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfigurationParser;
import pl.edu.icm.unity.saml.idp.SamlIdpStatisticReporter.SamlIdpStatisticReporterFactory;
import pl.edu.icm.unity.saml.idp.web.SamlAuthVaadinEndpoint;
import pl.edu.icm.unity.saml.idp.web.filter.ErrorHandler;
import pl.edu.icm.unity.saml.metadata.srv.RemoteMetadataService;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessorFactory;
import pl.edu.icm.unity.saml.slo.SLOReplyInstaller;
import pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilter;

import javax.servlet.Servlet;


/**
 * Extends a simple {@link SamlAuthVaadinEndpoint}, changing the SAML parse servlet to 
 * {@link SamlETDParseServlet}. 
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class SamlAuthETDVaadinEndpoint extends SamlAuthVaadinEndpoint
{
	public static final String SAML_CONSUMER_SERVLET_PATH = "/saml2unicoreIdp-web";
	
	@Autowired
	public SamlAuthETDVaadinEndpoint(NetworkServer server,
			ApplicationContext applicationContext,
			FreemarkerAppHandler freemarkerHandler,
			@Qualifier("insecure") PKIManagement pkiManagement,
			ExecutorsService executorsService,
			UnityServerConfiguration mainConfig,
			SAMLLogoutProcessorFactory logoutProcessorFactory,
			SLOReplyInstaller sloReplyInstaller,
			UnicoreIdpConsentDeciderServlet.Factory dispatcherServletFactory,
			MessageSource msg,
			AttributeTypeSupport aTypeSupport,
			RemoteMetadataService metadataService,
			URIAccessService uriAccessService,
			AdvertisedAddressProvider advertisedAddrProvider,
			RemoteRedirectedAuthnResponseProcessingFilter remoteAuthnResponseProcessingFilter,
			SamlIdpStatisticReporterFactory idpStatisticReporterFactory,
			LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement,
			SAMLIdPConfigurationParser samlIdPConfigurationParser
			)
	{
		super(SAML_CONSUMER_SERVLET_PATH, server, advertisedAddrProvider, applicationContext, freemarkerHandler,
				SamlUnicoreIdPWebUI.class, pkiManagement, executorsService, dispatcherServletFactory,
				logoutProcessorFactory, sloReplyInstaller, msg, aTypeSupport, metadataService, uriAccessService,
				remoteAuthnResponseProcessingFilter, idpStatisticReporterFactory, lastAccessAttributeManagement,
				samlIdPConfigurationParser);
	}

	@Override
	protected Servlet getSamlParseServlet(String endpointURL, String uiUrl)
	{
		return new SamlETDParseServlet(myMetadataManager, 
				endpointURL, uiUrl, new ErrorHandler(aTypeSupport, lastAccessAttributeManagement, freemarkerHandler));
	}
}
