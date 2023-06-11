/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.ws.console;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.utils.tprofile.OutputTranslationProfileFieldFactory;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFileConfigurationManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.saml.idp.ws.SamlSoapEndpoint;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.console.services.idp.IdpUsersHelper;

@Component
class SAMLSoapServiceController extends SAMLSoapServiceControllerBase
{
	@Autowired
	public SAMLSoapServiceController(MessageSource msg,
			EndpointManagement endpointMan,
			MessageSource msg2,
			EndpointManagement endpointMan2,
			RealmsManagement realmsMan,
			AuthenticationFlowManagement flowsMan,
			AuthenticatorManagement authMan,
			AttributeTypeManagement atMan,
			BulkGroupQueryService bulkService,
			URIAccessService uriAccessService,
			FileStorageService fileStorageService,
			UnityServerConfiguration serverConfig,
			IdentityTypeSupport idTypeSupport,
			PKIManagement pkiMan,
			NetworkServer server,
			OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory,
			ImageAccessService imageAccessService,
			IdpUsersHelper idpUserHelper, 
			AdvertisedAddressProvider advertisedAddrProvider,
			EndpointFileConfigurationManagement serviceFileConfigController)
	{
		super(msg, endpointMan, msg2, endpointMan2, realmsMan, flowsMan, authMan, atMan, imageAccessService,
				bulkService, uriAccessService, fileStorageService, serverConfig, idTypeSupport, pkiMan, server,
				outputTranslationProfileFieldFactory, idpUserHelper, advertisedAddrProvider, serviceFileConfigController);
	}

	@Override
	public EndpointTypeDescription getType()
	{
		return SamlSoapEndpoint.Factory.TYPE;
	}
	
}
