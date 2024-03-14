/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.console;

import io.imunity.console.utils.tprofile.OutputTranslationProfileFieldFactory;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.auth.services.DefaultServicesControllerBase;
import io.imunity.vaadin.auth.services.ServiceEditor;
import io.imunity.vaadin.auth.services.idp.IdpServiceController;
import io.imunity.vaadin.auth.services.idp.IdpUsersHelper;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.exceptions.EngineException;
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

import java.util.stream.Collectors;

abstract class SAMLServiceControllerBase extends DefaultServicesControllerBase implements IdpServiceController
{
	private final RealmsManagement realmsMan;
	private final AuthenticationFlowManagement flowsMan;
	private final AuthenticatorManagement authMan;
	private final AttributeTypeManagement atMan;
	private final BulkGroupQueryService bulkService;
	private final RegistrationsManagement registrationMan;
	private final URIAccessService uriAccessService;
	private final FileStorageService fileStorageService;
	private final UnityServerConfiguration serverConfig;
	private final AuthenticatorSupportService authenticatorSupportService;
	private final IdentityTypeSupport idTypeSupport;
	private final PKIManagement pkiMan;
	private final AdvertisedAddressProvider advertisedAddrProvider;
	private final OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory;
	private final IdpUsersHelper idpUserHelper;
	private final VaadinLogoImageLoader imageAccessService;
	private final PolicyDocumentManagement policyDocumentManagement;
	private final NetworkServer server;
	private final NotificationPresenter notificationPresenter;

	public SAMLServiceControllerBase(MessageSource msg,
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
			AdvertisedAddressProvider advertisedAddrProvider,
			NetworkServer server,
			OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory,
			IdpUsersHelper idpUserHelper,
			VaadinLogoImageLoader imageAccessService,
			PolicyDocumentManagement policyDocumentManagement,
			EndpointFileConfigurationManagement serviceFileConfigController,
			NotificationPresenter notificationPresenter)
	{
		super(msg, endpointMan, serviceFileConfigController);
		this.realmsMan = realmsMan;
		this.flowsMan = flowsMan;
		this.authMan = authMan;
		this.atMan = atMan;
		this.bulkService = bulkService;
		this.registrationMan = registrationMan;
		this.uriAccessService = uriAccessService;
		this.fileStorageService = fileStorageService;
		this.serverConfig = serverConfig;
		this.authenticatorSupportService = authenticatorSupportService;
		this.idTypeSupport = idTypeSupport;
		this.pkiMan = pkiMan;
		this.advertisedAddrProvider = advertisedAddrProvider;
		this.server = server;
		this.outputTranslationProfileFieldFactory = outputTranslationProfileFieldFactory;
		this.idpUserHelper = idpUserHelper;
		this.imageAccessService = imageAccessService;
		this.policyDocumentManagement = policyDocumentManagement;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public String getSupportedEndpointType()
	{
		return getType().getName();
	}

	@Override
	public ServiceEditor getEditor(SubViewSwitcher subViewSwitcher) throws EngineException
	{
		return new SAMLServiceEditor(msg, getType(), pkiMan, subViewSwitcher,
				outputTranslationProfileFieldFactory, advertisedAddrProvider.get().toString(),
				server.getUsedContextPaths(), uriAccessService, imageAccessService, 
				fileStorageService, serverConfig,
				realmsMan.getRealms().stream().map(r -> r.getName()).collect(Collectors.toList()),
				flowsMan.getAuthenticationFlows().stream().collect(Collectors.toList()),
				authMan.getAuthenticators(null).stream().collect(Collectors.toList()),
				atMan.getAttributeTypes().stream().map(a -> a.getName()).collect(Collectors.toList()),
				bulkService.getGroupAndSubgroups(bulkService.getBulkStructuralData("/")).values()
						.stream().map(g -> g.getGroup()).collect(Collectors.toList()),
				idpUserHelper.getAllUsers(),
				registrationMan.getForms().stream().filter(r -> r.isPubliclyAvailable())
						.map(r -> r.getName()).collect(Collectors.toList()),
				pkiMan.getCredentialNames(), pkiMan.getValidatorNames(), authenticatorSupportService,
				idTypeSupport.getIdentityTypes(), endpointMan.getEndpoints().stream()
						.map(e -> e.getContextAddress()).collect(Collectors.toList()),
						policyDocumentManagement.getPolicyDocuments(), notificationPresenter);
	}

	public abstract EndpointTypeDescription getType();

}
