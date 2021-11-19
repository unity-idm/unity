/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.unicore.samlidp.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.utils.tprofile.OutputTranslationProfileFieldFactory;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
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
import pl.edu.icm.unity.saml.idp.console.SAMLServiceControllerBase;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.console.services.idp.IdpUsersHelper;

@Component
public class SAMLUnicoreServiceController extends SAMLServiceControllerBase
{
	@Autowired
	public SAMLUnicoreServiceController(MessageSource msg,
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
			FileStorageService fileStorageService,
			UnityServerConfiguration serverConfig,
			AuthenticatorSupportService authenticatorSupportService,
			IdentityTypeSupport idTypeSupport,
			PKIManagement pkiMan,
			NetworkServer server,
			OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory,
			IdpUsersHelper idpUserHelper,
			ImageAccessService imageAccessService,
			AdvertisedAddressProvider advertisedAddrProvider, 
			PolicyDocumentManagement policyDocumentManagement,
			EndpointFileConfigurationManagement serviceFileConfigController)
	{
		super(msg, endpointMan, msg2, endpointMan2, realmsMan, flowsMan, authMan, atMan, bulkService, registrationMan,
				uriAccessService, fileStorageService, serverConfig, authenticatorSupportService, idTypeSupport, pkiMan,
				advertisedAddrProvider, server, outputTranslationProfileFieldFactory, idpUserHelper,
				imageAccessService, policyDocumentManagement, serviceFileConfigController);
	}

	@Override
	public EndpointTypeDescription getType()
	{
		return SamlUnicoreIdPWebEndpointFactory.TYPE;
	}
}
