/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.console;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.google.common.base.Functions;

import io.imunity.attr.introspection.AttrInstrospectionEndpointFactory;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFileConfigurationManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.types.authn.IdPInfo;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.console.services.DefaultServicesControllerBase;
import pl.edu.icm.unity.webui.console.services.ServiceController;
import pl.edu.icm.unity.webui.console.services.ServiceEditor;

/**
 * Attribute instrospection service controller. Based on the standard web
 * service editor
 * 
 * @author P.Piernik
 *
 */
@Component
class AttrIntrospectionServiceController extends DefaultServicesControllerBase implements ServiceController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_ATTR_INTROSPECTION,
			AttrIntrospectionServiceController.class);

	private final NetworkServer server;
	private final AuthenticationFlowManagement flowsMan;
	private final AuthenticatorManagement authMan;
	private final AuthenticatorSupportService authenticatorSupportService;
	private final URIAccessService uriAccessService;
	private final UnityServerConfiguration serverConfig;
	private final ImageAccessService imageAccessService;
	private final FileStorageService fileStorageService;

	AttrIntrospectionServiceController(MessageSource msg, EndpointManagement endpointMan, NetworkServer server,
			EndpointFileConfigurationManagement serviceFileConfigController, AuthenticationFlowManagement flowsMan,
			AuthenticatorManagement authMan, AuthenticatorSupportService authenticatorSupportService,
			URIAccessService uriAccessService, UnityServerConfiguration serverConfig,
			ImageAccessService imageAccessService, FileStorageService fileStorageService)
	{
		super(msg, endpointMan, serviceFileConfigController);
		this.server = server;
		this.authMan = authMan;
		this.flowsMan = flowsMan;
		this.authenticatorSupportService = authenticatorSupportService;
		this.uriAccessService = uriAccessService;
		this.serverConfig = serverConfig;
		this.imageAccessService = imageAccessService;
		this.fileStorageService = fileStorageService;
	}

	@Override
	public String getSupportedEndpointType()
	{
		return AttrInstrospectionEndpointFactory.NAME;
	}

	@Override
	public ServiceEditor getEditor(SubViewSwitcher subViewSwitcher) throws EngineException
	{

		return new AttrIntrospectionServiceEditor(msg,
				endpointMan.getEndpoints().stream().map(e -> e.getContextAddress()).collect(Collectors.toList()),
				server.getUsedContextPaths(), authenticatorSupportService, () -> getRemoteAuthnOptions(),
				() -> getIdPs(), uriAccessService, serverConfig, imageAccessService, fileStorageService);
	}

	private List<IdPInfo> getIdPs()
	{
		List<AuthenticatorInstance> remoteAuthenticators;
		List<IdPInfo> providers = new ArrayList<>();
		try
		{
			remoteAuthenticators = authenticatorSupportService.getRemoteAuthenticators(VaadinAuthentication.NAME);
		} catch (EngineException e)
		{
			log.error("Can not get authenticators instances", e);
			return providers;
		}

		remoteAuthenticators.forEach(a -> providers.addAll(a.extractIdPs()));

		return providers;
	}

	private List<String> getRemoteAuthnOptions()
	{
		List<String> authnOptions = new ArrayList<>();
		Map<String, AuthenticatorInfo> authenticatorsMap = getAuthenticators();

		authnOptions.addAll(authenticatorsMap.values().stream().filter(
				a -> a.getSupportedBindings().contains(VaadinAuthentication.NAME) && !a.getTypeDescription().isLocal())
				.map(a -> a.getId()).collect(Collectors.toList()));
		try
		{
			for (AuthenticationFlowDefinition f : flowsMan.getAuthenticationFlows())
			{
				boolean supportsBinding = true;
				for (String authenticatorName : f.getAllAuthenticators())
				{
					AuthenticatorInfo authenticatorInfo = authenticatorsMap.get(authenticatorName);

					if (authenticatorInfo.getTypeDescription().isLocal()
							|| !authenticatorInfo.getSupportedBindings().contains(VaadinAuthentication.NAME))
					{
						supportsBinding = false;
						break;
					}
				}
				if (supportsBinding)
					authnOptions.add(f.getName());
			}
		} catch (EngineException e)
		{
			log.error("Can not get authentication flows", e);
			return authnOptions;
		}

		return authnOptions;
	}

	private Map<String, AuthenticatorInfo> getAuthenticators()
	{
		try
		{
			return authMan.getAuthenticators(null).stream()
					.collect(Collectors.toMap(AuthenticatorInfo::getId, Functions.identity()));
		} catch (EngineException e)
		{
			log.error("Can not get authenticators", e);
			return Collections.emptyMap();
		}
	}

}
