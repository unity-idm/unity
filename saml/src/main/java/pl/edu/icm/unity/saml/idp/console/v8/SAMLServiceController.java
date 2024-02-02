/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.console.v8;

import io.imunity.webconsole.utils.tprofile.OutputTranslationProfileFieldFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.*;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFileConfigurationManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.saml.idp.web.SamlIdPWebEndpointFactory;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.console.services.idp.IdpUsersHelper;

@Component("SAMLServiceControllerV8")
public class SAMLServiceController extends SAMLServiceControllerBase
{
	@Autowired
	public SAMLServiceController(MessageSource msg,
			EndpointManagement endpointMan,
			MessageSource msg2,
			EndpointManagement endpointMan2,
			RealmsManagement realmsMan,
			AuthenticationFlowManagement flowsMan,
			AuthenticatorManagement authMan,
			AttributeTypeManagement atMan,
			BulkGroupQueryService bulkService,
			RegistrationsManagement registrationMan,
			URIAccessService uriAccessService,
			ImageAccessService imageAccessService,
			FileStorageService fileStorageService,
			UnityServerConfiguration serverConfig,
			AuthenticatorSupportService authenticatorSupportService,
			IdentityTypeSupport idTypeSupport,
			PKIManagement pkiMan,
			NetworkServer server,
			AdvertisedAddressProvider advertisedAddrProvider,
			OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory,
			IdpUsersHelper idpUserHelper,
			PolicyDocumentManagement policyDocumentManagement, 
			EndpointFileConfigurationManagement serviceFileConfigController)
	{
		super(msg, endpointMan, msg2, endpointMan2, realmsMan, flowsMan, authMan, atMan, bulkService,
				registrationMan, uriAccessService, fileStorageService, serverConfig,
				authenticatorSupportService, idTypeSupport, pkiMan, advertisedAddrProvider, server,
				outputTranslationProfileFieldFactory, idpUserHelper, imageAccessService, policyDocumentManagement,
				serviceFileConfigController);
	}

	@Override
	public EndpointTypeDescription getType()
	{
		return SamlIdPWebEndpointFactory.TYPE;
	}

}
