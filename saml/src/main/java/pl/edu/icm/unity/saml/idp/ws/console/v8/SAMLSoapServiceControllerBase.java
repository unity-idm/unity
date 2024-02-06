/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.ws.console.v8;

import io.imunity.webconsole.utils.tprofile.OutputTranslationProfileFieldFactory;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.*;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFileConfigurationManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.console.services.DefaultServicesControllerBase;
import pl.edu.icm.unity.webui.console.services.ServiceEditor;
import pl.edu.icm.unity.webui.console.services.idp.IdpServiceController;
import pl.edu.icm.unity.webui.console.services.idp.IdpUsersHelper;

import java.util.stream.Collectors;

abstract class SAMLSoapServiceControllerBase extends DefaultServicesControllerBase
		implements IdpServiceController
{
	private RealmsManagement realmsMan;
	private AuthenticationFlowManagement flowsMan;
	private AuthenticatorManagement authMan;
	private AttributeTypeManagement atMan;
	private BulkGroupQueryService bulkService;
	private URIAccessService uriAccessService;
	private FileStorageService fileStorageService;
	private UnityServerConfiguration serverConfig;
	private IdentityTypeSupport idTypeSupport;
	private PKIManagement pkiMan;
	private NetworkServer server;
	private OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory;
	private IdpUsersHelper idpUserHelper;
	private ImageAccessService imageAccessService;
	private AdvertisedAddressProvider advertisedAddrProvider;

	public SAMLSoapServiceControllerBase(MessageSource msg, EndpointManagement endpointMan, MessageSource msg2,
			EndpointManagement endpointMan2, RealmsManagement realmsMan, AuthenticationFlowManagement flowsMan,
			AuthenticatorManagement authMan, AttributeTypeManagement atMan, ImageAccessService imageAccessService,
			BulkGroupQueryService bulkService, URIAccessService uriAccessService, FileStorageService fileStorageService,
			UnityServerConfiguration serverConfig, IdentityTypeSupport idTypeSupport, PKIManagement pkiMan,
			NetworkServer server, OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory,
			IdpUsersHelper idpUserHelper, AdvertisedAddressProvider advertisedAddrProvider,
			EndpointFileConfigurationManagement serviceFileConfigController)
	{
		super(msg, endpointMan, serviceFileConfigController);
		this.realmsMan = realmsMan;
		this.flowsMan = flowsMan;
		this.authMan = authMan;
		this.atMan = atMan;
		this.imageAccessService = imageAccessService;
		this.bulkService = bulkService;
		this.uriAccessService = uriAccessService;
		this.fileStorageService = fileStorageService;
		this.serverConfig = serverConfig;
		this.idTypeSupport = idTypeSupport;
		this.pkiMan = pkiMan;
		this.server = server;
		this.outputTranslationProfileFieldFactory = outputTranslationProfileFieldFactory;
		this.idpUserHelper = idpUserHelper;
		this.advertisedAddrProvider = advertisedAddrProvider;
	}

	@Override
	public String getSupportedEndpointType()
	{
		return getType().getName();
	}

	@Override
	public ServiceEditor getEditor(SubViewSwitcher subViewSwitcher) throws EngineException
	{
		return new SAMLSoapServiceEditor(msg, getType(), pkiMan, subViewSwitcher,
				outputTranslationProfileFieldFactory, advertisedAddrProvider.get().toString(),
				server.getUsedContextPaths(), uriAccessService, imageAccessService, fileStorageService, serverConfig,
				realmsMan.getRealms().stream().map(r -> r.getName()).collect(Collectors.toList()),
				flowsMan.getAuthenticationFlows().stream().collect(Collectors.toList()),
				authMan.getAuthenticators(null).stream().collect(Collectors.toList()),
				atMan.getAttributeTypes().stream().map(a -> a.getName()).collect(Collectors.toList()),
				bulkService.getGroupAndSubgroups(bulkService.getBulkStructuralData("/")).values()
						.stream().map(g -> g.getGroup()).collect(Collectors.toList()),
				idpUserHelper.getAllUsers(), pkiMan.getCredentialNames(), pkiMan.getValidatorNames(),
				idTypeSupport.getIdentityTypes(), endpointMan.getEndpoints().stream()
						.map(e -> e.getContextAddress()).collect(Collectors.toList()));
	}

	public abstract EndpointTypeDescription getType();

}
