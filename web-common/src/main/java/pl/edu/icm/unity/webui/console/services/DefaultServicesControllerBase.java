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
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Base for all standard servive controllers. Operates on
 * {@link DefaultServiceDefinition}
 * 
 * @author P.Piernik
 *
 */
public abstract class DefaultServicesControllerBase implements ServiceControllerBaseInt
{
	protected final MessageSource msg;
	protected final EndpointManagement endpointMan;
	protected final ServiceFileConfigurationController serviceFileConfigController;
	
	public DefaultServicesControllerBase(MessageSource msg, EndpointManagement endpointMan,
			ServiceFileConfigurationController serviceFileConfigController)
	{
		this.msg = msg;
		this.endpointMan = endpointMan;
		this.serviceFileConfigController = serviceFileConfigController;
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
				DefaultServiceDefinition service = getServiceDef(endpoint);
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

			return getServiceDef(endpoint);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.getError", name), e);
		}
	}

	@Override
	public void reloadFromConfig(ServiceDefinition service) throws ControllerException
	{
		EndpointConfiguration config = serviceFileConfigController.getEndpointConfig(service.getName());
		try
		{
			endpointMan.updateEndpoint(service.getName(), config);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.updateError", service.getName()), e);
		}
	}
	
	private DefaultServiceDefinition getServiceDef(Endpoint endpoint) throws ControllerException
	{
		DefaultServiceDefinition serviceDef = new DefaultServiceDefinition(endpoint.getTypeId());
		serviceDef.setName(endpoint.getName());
		serviceDef.setAddress(endpoint.getContextAddress());
		serviceDef.setConfiguration(endpoint.getConfiguration().getConfiguration());
		serviceDef.setAuthenticationOptions(endpoint.getConfiguration().getAuthenticationOptions());
		serviceDef.setDisplayedName(endpoint.getConfiguration().getDisplayedName());
		serviceDef.setRealm(endpoint.getConfiguration().getRealm());
		serviceDef.setDescription(endpoint.getConfiguration().getDescription());
		serviceDef.setState(endpoint.getState());
		serviceDef.setBinding(getBinding(endpoint.getTypeId()));
		serviceDef.setSupportFromConfigReload(serviceFileConfigController.getEndpointConfigKey(endpoint.getName()).isPresent());
		return serviceDef;
	}
	
	private String getBinding(String typeId) throws ControllerException
	{
		EndpointTypeDescription type;
		try
		{
			type = endpointMan.getEndpointTypes().stream().filter(t -> t.getName().equals(typeId))
					.findFirst().orElse(null);
		} catch (EngineException e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.getTypeError", typeId), e);
		}
		if (type == null)
		{
			return null;
		} else
		{
			return type.getSupportedBinding();
		}
	}

}
