/*
 * Copyright (c) 2024 Bixbit s.c. All rights reserved.
 *  See LICENSE file for licensing information.
 */
package pl.edu.icm.unity.webui.console.services;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFileConfigurationManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

public class DefaultServiceDefinitionResolver
{
	private final EndpointManagement endpointMan;
	private final EndpointFileConfigurationManagement serviceFileConfigController;
	private final MessageSource msg;
	
	public DefaultServiceDefinitionResolver(EndpointManagement endpointMan,
			EndpointFileConfigurationManagement serviceFileConfigController, MessageSource msg)
	{
		this.endpointMan = endpointMan;
		this.serviceFileConfigController = serviceFileConfigController;
		this.msg = msg;
	}

	public DefaultServiceDefinition resolve(Endpoint endpoint) throws ControllerException
	{
		return resolve(endpoint, getBinding(endpoint.getTypeId()));
	}
	
	public DefaultServiceDefinition resolve(Endpoint endpoint, String binding)
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
		serviceDef.setBinding(binding);
		serviceDef.setSupportsConfigReloadFromFile(serviceFileConfigController.getEndpointConfigKey(endpoint.getName()).isPresent());
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
