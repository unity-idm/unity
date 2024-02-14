/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFileConfigurationManagement;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Base for all standard service controllers. Operates on
 * {@link DefaultServiceDefinition}
 * 
 * @author P.Piernik
 *
 */
public abstract class DefaultServicesControllerBase implements ServiceControllerBaseInt
{
	protected final MessageSource msg;
	protected final EndpointManagement endpointMan;
	protected final EndpointFileConfigurationManagement serviceFileConfigController;
	private final DefaultServiceDefinitionResolver serviceDefinitionResolver;
	
	public DefaultServicesControllerBase(MessageSource msg, EndpointManagement endpointMan,
			EndpointFileConfigurationManagement serviceFileConfigController)
	{
		this.msg = msg;
		this.endpointMan = endpointMan;
		this.serviceFileConfigController = serviceFileConfigController;
		this.serviceDefinitionResolver = new DefaultServiceDefinitionResolver(endpointMan,
				serviceFileConfigController, msg);
	}

	@Override
	public List<ServiceDefinition> getServices() throws ControllerException
	{
		List<ServiceDefinition> ret = new ArrayList<>();
		try
		{
			for (Endpoint endpoint : endpointMan.getEndpoints().stream()
					.filter(e -> e.getTypeId().equals(getSupportedEndpointType()))
					.collect(Collectors.toList()))
			{
				DefaultServiceDefinition service = serviceDefinitionResolver.resolve(endpoint);
				ret.add(service);
			}
			return ret;

		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.getAllError"), e);
		}
	}

	@Override
	public void deploy(ServiceDefinition service) throws ControllerException
	{
		DefaultServiceDefinition def = (DefaultServiceDefinition) service;

		try
		{
			EndpointConfiguration wconfig = new EndpointConfiguration(def.getDisplayedName(),
					def.getDescription(), def.getAuthenticationOptions(), def.getConfiguration(),
					def.getRealm());
			endpointMan.deploy(service.getType(), service.getName(), def.getAddress(), wconfig);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.deployError", def.getName()),
					e);
		}

	}

	@Override
	public void undeploy(ServiceDefinition service) throws ControllerException
	{
		DefaultServiceDefinition def = (DefaultServiceDefinition) service;

		try
		{
			endpointMan.undeploy(def.getName());

		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.undeployError", def.getName()),
					e);
		}

	}

	@Override
	public void update(ServiceDefinition service) throws ControllerException
	{
		DefaultServiceDefinition def = (DefaultServiceDefinition) service;

		try
		{
			EndpointConfiguration wconfig = new EndpointConfiguration(def.getDisplayedName(),
					def.getDescription(), def.getAuthenticationOptions(), def.getConfiguration(),
					def.getRealm());
			endpointMan.updateEndpoint(def.getName(), wconfig);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.updateError", def.getName()),
					e);
		}

	}

	@Override
	public void remove(ServiceDefinition service) throws ControllerException
	{
		try
		{
			endpointMan.removeEndpoint(service.getName());

		} catch (Exception e)
		{

			throw new ControllerException(
					msg.getMessage("ServicesController.removeError", service.getName()), e);
		}

	}

	@Override
	public ServiceDefinition getService(String name) throws ControllerException
	{
		try
		{
			Endpoint endpoint = endpointMan.getEndpoints().stream()
					.filter(e -> e.getName().equals(name)
							&& e.getTypeId().equals(getSupportedEndpointType()))
					.findFirst().orElse(null);
			if (endpoint == null)
				return null;

			return serviceDefinitionResolver.resolve(endpoint);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.getError", name), e);
		}
	}

	@Override
	public void reloadConfigFromFile(ServiceDefinition service) throws ControllerException
	{
		try
		{
			endpointMan.updateEndpoint(service.getName(),
					serviceFileConfigController.getEndpointConfig(service.getName()));
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.updateError", service.getName()), e);
		}
	}
}
