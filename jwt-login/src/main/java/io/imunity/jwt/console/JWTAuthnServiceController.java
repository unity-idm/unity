/*
 * Copyright (c) 2024 Bixbit s.c. All rights reserved.
 *  See LICENSE file for licensing information.
 */
package io.imunity.jwt.console;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import io.imunity.jwt.AuthzLoginTokenEndpoint;
import io.imunity.jwt.JWTAuthzWebEndpointFactory;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFileConfigurationManagement;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinitionResolver;
import pl.edu.icm.unity.webui.console.services.ServiceController;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditor;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

@Component
class JWTAuthnServiceController implements ServiceController
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, JWTAuthnServiceController.class);
	private final RealmsManagement realmsMan;
	private final AuthenticationFlowManagement flowsMan;
	private final AuthenticatorManagement authMan;
	private final PKIManagement pkiMan;
	private final NetworkServer networkServer;
	private final MessageSource msg;
	private final EndpointManagement endpointMan;
	private final EndpointFileConfigurationManagement serviceFileConfigController;
	private final DefaultServiceDefinitionResolver serviceDefinitionResolver;

	JWTAuthnServiceController(MessageSource msg,
			EndpointManagement endpointMan,
			RealmsManagement realmsMan,
			AuthenticationFlowManagement flowsMan,
			AuthenticatorManagement authMan,
			PKIManagement pkiMan,
			NetworkServer networkServer,
			EndpointFileConfigurationManagement serviceFileConfigController)
	{
		this.realmsMan = realmsMan;
		this.flowsMan = flowsMan;
		this.authMan = authMan;
		this.pkiMan = pkiMan;
		this.networkServer = networkServer;
		this.msg = msg;
		this.endpointMan = endpointMan;
		this.serviceFileConfigController = serviceFileConfigController;
		this.serviceDefinitionResolver = new DefaultServiceDefinitionResolver(endpointMan, serviceFileConfigController, msg);
	}

	@Override
	public String getSupportedEndpointType()
	{
		return JWTAuthzWebEndpointFactory.NAME;
	}

	@Override
	public ServiceEditor getEditor(SubViewSwitcher subViewSwitcher) throws EngineException
	{
		return new JWTAuthnServiceEditor(msg,
				realmsMan.getRealms().stream().map(r -> r.getName()).collect(Collectors.toList()),
				flowsMan.getAuthenticationFlows().stream().collect(Collectors.toList()),
				authMan.getAuthenticators(null).stream().collect(Collectors.toList()), pkiMan.getCredentialNames(),
				endpointMan.getEndpoints().stream().map(e -> e.getContextAddress()).collect(Collectors.toList()),
				networkServer.getUsedContextPaths());
	}

	@Override
	public void deploy(ServiceDefinition service) throws ControllerException
	{
		JWTAuthnServiceDefinition def = (JWTAuthnServiceDefinition) service;
		DefaultServiceDefinition webAuthzService = def.webAuthzService;
		DefaultServiceDefinition tokenService = def.tokenService;
		String tag = UUID.randomUUID().toString();
		try
		{
			EndpointConfiguration wconfig = new EndpointConfiguration(webAuthzService.getDisplayedName(),
					webAuthzService.getDescription(), webAuthzService.getAuthenticationOptions(),
					webAuthzService.getConfiguration(), webAuthzService.getRealm(), tag);
			endpointMan.deploy(webAuthzService.getType(), webAuthzService.getName(), webAuthzService.getAddress(),
					wconfig);
			LOG.debug("Deployed: {}, {}", webAuthzService, wconfig);
			
			EndpointConfiguration rconfig = new EndpointConfiguration(tokenService.getDisplayedName(),
					tokenService.getDescription(), tokenService.getAuthenticationOptions(),
					tokenService.getConfiguration(), tokenService.getRealm(), tag);
			endpointMan.deploy(tokenService.getType(), tokenService.getName(), tokenService.getAddress(), rconfig);
			LOG.debug("Deployed: {}, {}", tokenService, rconfig);

		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.deployError", webAuthzService.getName()),
					e);
		}
	}

	@Override
	public void undeploy(ServiceDefinition service) throws ControllerException
	{
		JWTAuthnServiceDefinition def = (JWTAuthnServiceDefinition) service;
		DefaultServiceDefinition webAuthzService = def.webAuthzService;
		DefaultServiceDefinition tokenService = def.tokenService;

		try
		{
			endpointMan.undeploy(webAuthzService.getName());
			endpointMan.undeploy(tokenService.getName());

		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.undeployError", webAuthzService.getName()),
					e);
		}
	}

	@Override
	public List<ServiceDefinition> getServices() throws ControllerException
	{
		List<ServiceDefinition> ret = new ArrayList<>();
		try
		{
			for (Endpoint endpoint : endpointMan.getEndpoints().stream()
					.filter(e -> e.getTypeId().equals(JWTAuthzWebEndpointFactory.TYPE.getName()))
					.collect(Collectors.toList()))
			{
				DefaultServiceDefinition webService = serviceDefinitionResolver.resolve(endpoint,
						JWTAuthzWebEndpointFactory.TYPE.getSupportedBinding());
				DefaultServiceDefinition tokenService = getTokenService(endpoint.getConfiguration().getTag());
				ret.add(new JWTAuthnServiceDefinition(webService, tokenService));
			}
			return ret;

		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.getAllError"), e);
		}
	}

	private DefaultServiceDefinition getTokenService(String tag) throws EngineException
	{
		List<Endpoint> matchingTokenEndpoints = endpointMan.getEndpoints().stream()
				.filter(e -> e.getTypeId().equals(AuthzLoginTokenEndpoint.TYPE.getName())
						&& e.getConfiguration().getTag().equals(tag))
				.collect(Collectors.toList());
		if (matchingTokenEndpoints.isEmpty())
		{
			LOG.warn("Can not find a corresponding token endpoint for JWT authz {}", tag);
			return null;
		}
		if (matchingTokenEndpoints.size() > 1)
		{
			LOG.warn("Found {} token endpoints for JWT authz endpoint with tag {}", matchingTokenEndpoints.size(), tag);
			return null;
		}

		DefaultServiceDefinition tokenService = serviceDefinitionResolver.resolve(matchingTokenEndpoints.get(0), 
				AuthzLoginTokenEndpoint.TYPE.getSupportedBinding());
		return tokenService;
	}
	
	@Override
	public ServiceDefinition getService(String name) throws ControllerException
	{
		try
		{
			Endpoint endpoint = endpointMan.getEndpoints().stream().filter(
					e -> e.getName().equals(name) && e.getTypeId().equals(JWTAuthzWebEndpointFactory.TYPE.getName()))
					.findFirst().orElse(null);

			if (endpoint == null)
			{
				return null;
			}

			DefaultServiceDefinition webService = serviceDefinitionResolver.resolve(endpoint, 
					JWTAuthzWebEndpointFactory.TYPE.getSupportedBinding());
			JWTAuthnServiceDefinition def = new JWTAuthnServiceDefinition(webService,
					getTokenService(endpoint.getConfiguration().getTag()));
			return def;
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.getError", name), e);
		}
	}

	@Override
	public void update(ServiceDefinition service) throws ControllerException
	{
		JWTAuthnServiceDefinition def = (JWTAuthnServiceDefinition) service;
		DefaultServiceDefinition webAuthzService = def.webAuthzService;
		DefaultServiceDefinition tokenService = def.tokenService;
		String tag = UUID.randomUUID().toString();
		try
		{
			EndpointConfiguration wconfig = new EndpointConfiguration(webAuthzService.getDisplayedName(),
					webAuthzService.getDescription(), webAuthzService.getAuthenticationOptions(),
					webAuthzService.getConfiguration(), webAuthzService.getRealm(), tag);
			endpointMan.updateEndpoint(webAuthzService.getName(), wconfig);
			
			EndpointConfiguration rconfig = new EndpointConfiguration(tokenService.getDisplayedName(),
					tokenService.getDescription(), tokenService.getAuthenticationOptions(),
					tokenService.getConfiguration(), tokenService.getRealm(), tag);
			endpointMan.updateEndpoint(tokenService.getName(), rconfig);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.updateError", def.getName()), e);
		}
	}

	@Override
	public void remove(ServiceDefinition service) throws ControllerException
	{
		JWTAuthnServiceDefinition def = (JWTAuthnServiceDefinition) service;
		DefaultServiceDefinition webAuthzService = def.webAuthzService;
		DefaultServiceDefinition tokenService = def.tokenService;

		try
		{
			endpointMan.removeEndpoint(webAuthzService.getName());
			endpointMan.removeEndpoint(tokenService.getName());
			
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.removeError", webAuthzService.getName()),
					e);
		}
	}

	@Override
	public void reloadConfigFromFile(ServiceDefinition service) throws ControllerException
	{
		JWTAuthnServiceDefinition def = (JWTAuthnServiceDefinition) service;
		DefaultServiceDefinition webAuthzService = def.webAuthzService;
		DefaultServiceDefinition tokenService = def.tokenService;

		List<ControllerException> exs = new ArrayList<>();
		try
		{
			endpointMan.updateEndpoint(webAuthzService.getName(),
					serviceFileConfigController.getEndpointConfig(webAuthzService.getName()));
		} catch (Exception e)
		{
			exs.add(new ControllerException(msg.getMessage("ServicesController.updateError", def.getName()), e));
		}

		try
		{
			endpointMan.updateEndpoint(tokenService.getName(),
					serviceFileConfigController.getEndpointConfig(tokenService.getName()));
		} catch (Exception e)
		{
			exs.add(new ControllerException(msg.getMessage("ServicesController.updateError", def.getName()), e));
		}

		if (exs.size() == 2)
		{
			LOG.error("Can not update JWT Authz endpoint", exs.get(2).getCause());
		}

		if (!exs.isEmpty())
		{
			throw exs.get(0);
		}
	}
}
