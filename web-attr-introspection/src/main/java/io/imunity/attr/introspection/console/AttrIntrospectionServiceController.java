/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.console;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.google.common.base.Functions;

import io.imunity.attr.introspection.AttrIntrospectionEndpointFactory;
import io.imunity.vaadin.auth.VaadinAuthentication;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.auth.services.DefaultServicesControllerBase;
import io.imunity.vaadin.auth.services.ServiceController;
import io.imunity.vaadin.auth.services.ServiceEditor;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInfo;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.IdPInfo;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFileConfigurationManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.server.NetworkServer;

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
	private final VaadinLogoImageLoader imageAccessService;
	private final FileStorageService fileStorageService;
	private final UnityServerConfiguration serverConfig;

	AttrIntrospectionServiceController(MessageSource msg, EndpointManagement endpointMan, NetworkServer server,
			EndpointFileConfigurationManagement serviceFileConfigController, AuthenticationFlowManagement flowsMan,
			AuthenticatorManagement authMan, AuthenticatorSupportService authenticatorSupportService,
			FileStorageService fileStorageService, VaadinLogoImageLoader imageAccessService,
			UnityServerConfiguration serverConfig)
	{
		super(msg, endpointMan, serviceFileConfigController);
		this.server = server;
		this.authMan = authMan;
		this.flowsMan = flowsMan;
		this.authenticatorSupportService = authenticatorSupportService;
		this.fileStorageService = fileStorageService;
		this.imageAccessService = imageAccessService;
		this.serverConfig = serverConfig;
	}

	@Override
	public String getSupportedEndpointType()
	{
		return AttrIntrospectionEndpointFactory.NAME;
	}

	@Override
	public ServiceEditor getEditor(SubViewSwitcher subViewSwitcher) throws EngineException
	{

		return new AttrIntrospectionServiceEditor(msg, endpointMan.getEndpoints()
				.stream()
				.map(e -> e.getContextAddress())
				.collect(Collectors.toList()),
				endpointMan.getEndpoints()
						.stream()
						.map(e -> e.getName())
						.collect(Collectors.toList()),
				server.getUsedContextPaths(), authenticatorSupportService, () -> getRemoteAuthnOptions(),
				() -> getIdPs(), fileStorageService, imageAccessService, serverConfig);
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

	private Set<String> getRemoteAuthnOptions()
	{
		Set<String> authnOptions = new HashSet<>();
		Map<String, AuthenticatorInfo> authenticatorsMap = getAuthenticators();

		authnOptions.addAll(authenticatorsMap.values()
				.stream()
				.filter(a -> a.getSupportedBindings()
						.contains(VaadinAuthentication.NAME)
						&& !a.getTypeDescription()
								.isLocal())
				.map(a -> a.getId())
				.collect(Collectors.toList()));
		try
		{
			for (AuthenticationFlowDefinition f : flowsMan.getAuthenticationFlows())
			{
				boolean supportsBinding = true;
				for (String authenticatorName : f.getAllAuthenticators())
				{
					AuthenticatorInfo authenticatorInfo = authenticatorsMap.get(authenticatorName);

					if (authenticatorInfo.getTypeDescription()
							.isLocal()
							|| !authenticatorInfo.getSupportedBindings()
									.contains(VaadinAuthentication.NAME))
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
			return authMan.getAuthenticators(null)
					.stream()
					.collect(Collectors.toMap(AuthenticatorInfo::getId, Functions.identity()));
		} catch (EngineException e)
		{
			log.error("Can not get authenticators", e);
			return Collections.emptyMap();
		}
	}

}
