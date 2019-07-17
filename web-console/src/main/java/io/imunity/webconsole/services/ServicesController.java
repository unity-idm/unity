/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.authn.endpoints.ServiceDefinition;
import pl.edu.icm.unity.webui.authn.endpoints.ServiceEditorComponent.ServiceEditorTab;
import pl.edu.icm.unity.webui.authn.endpoints.ServiceEndpointEditorFactoriesRegistry;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * 
 * @author P.Piernik
 *
 */
@Component
class ServicesController
{
	private UnityMessageSource msg;
	private EndpointManagement endpointMan;

	private ServiceEndpointEditorFactoriesRegistry editorsRegistry;

	ServicesController(UnityMessageSource msg, EndpointManagement endpointMan,
			ServiceEndpointEditorFactoriesRegistry editorsRegistry)
	{
		this.msg = msg;
		this.endpointMan = endpointMan;
		this.editorsRegistry = editorsRegistry;
	}

	List<EndpointEntry> getServices() throws ControllerException
	{
		List<EndpointEntry> ret = new ArrayList<>();

		try
		{
			Map<String, EndpointTypeDescription> endpointTypes = endpointMan.getEndpointTypes().stream()
					.collect(Collectors.toMap(EndpointTypeDescription::getName,
							Function.identity()));

			Set<String> supportedTypes = editorsRegistry.getAll().stream()
					.map(e -> e.getSupportedEndpointType()).collect(Collectors.toSet());

			for (Endpoint endpoint : endpointMan.getEndpoints().stream()
					.filter(e -> supportedTypes.contains(e.getTypeId()))
					.collect(Collectors.toList()))
			{
				ret.add(new EndpointEntry(endpoint, endpointTypes.get(endpoint.getTypeId())));
			}
			return ret;

		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.getAllError"), e);
		}
	}

	void undeploy(Endpoint endpoint) throws ControllerException
	{
		try
		{
			endpointMan.undeploy(endpoint.getName());

		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("ServicesController.undeployError", endpoint.getName()), e);
		}
	}

	void deploy(Endpoint endpoint) throws ControllerException
	{
		try
		{
			endpointMan.deploy(endpoint.getTypeId(), endpoint.getName(), endpoint.getContextAddress(),
					endpoint.getConfiguration());

		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("ServicesController.deployError", endpoint.getName()), e);
		}
	}

	void deploy(ServiceDefinition def) throws ControllerException
	{
		try
		{
			EndpointConfiguration config = new EndpointConfiguration(def.getDisplayedName(),
					def.getDescription(), def.getAuthenticationOptions(), def.getConfiguration(),
					def.getRealm());
			endpointMan.deploy(def.getTypeId(), def.getName(), def.getAddress(), config);

		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.deployError", def.getName()),
					e);
		}
	}

	void update(ServiceDefinition def) throws ControllerException
	{
		try
		{
			EndpointConfiguration config = new EndpointConfiguration(def.getDisplayedName(),
					def.getDescription(), def.getAuthenticationOptions(), def.getConfiguration(),
					def.getRealm());
			endpointMan.updateEndpoint(def.getName(), config);

		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.updateError", def.getName()),
					e);
		}
	}

	ServiceDefinition getService(String name) throws ControllerException
	{
		try
		{
			ResolvedEndpoint endpoint = endpointMan.getDeployedEndpoints().stream()
					.filter(e -> e.getName().equals(name)).findFirst().orElse(null);
			ServiceDefinition def = new ServiceDefinition(endpoint);
			return def;

		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.getError", name), e);
		}
	}

	void remove(Endpoint endpoint) throws ControllerException
	{
		try
		{
			endpointMan.removeEndpoint(endpoint.getName());

		} catch (Exception e)
		{

			throw new ControllerException(
					msg.getMessage("ServicesController.removeError", endpoint.getName()), e);
		}

	}

	MainServiceEditor getEditor(ServiceDefinition toEdit, ServiceEditorTab initTab) throws ControllerException
	{

		try
		{
			return new MainServiceEditor(msg, editorsRegistry, endpointMan.getEndpointTypes(), toEdit,
					initTab);
		} catch (EngineException e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.createEditorError"), e);
		}
	}

}
