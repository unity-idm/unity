/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole.services.base;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.console.services.ServiceControllerBaseInt;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent.ServiceEditorTab;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Common part for idp and standard service controllers
 * @author P.Piernik
 *
 */
public class ServiceControllerBase
{
	private MessageSource msg;
	private EndpointManagement endpointMan;
	private TypesRegistryBase<? extends ServiceControllerBaseInt> controllersRegistry;		
			
	public ServiceControllerBase(MessageSource msg, EndpointManagement endpointMan,
			TypesRegistryBase< ? extends ServiceControllerBaseInt> controllersRegistry)
	{
		this.msg = msg;
		this.endpointMan = endpointMan;
		this.controllersRegistry = controllersRegistry;
	}

	public List<ServiceDefinition> getServices() throws ControllerException
	{

		List<ServiceDefinition> ret = new ArrayList<>();
		
		for (ServiceControllerBaseInt controller : controllersRegistry.getAll())
		{
			ret.addAll(controller.getServices());
		}
		return ret;
	}

	public void undeploy(ServiceDefinition service) throws ControllerException
	{
		controllersRegistry.getByName(service.getType()).undeploy(service);
	}

	public void deploy(ServiceDefinition service) throws ControllerException
	{

		controllersRegistry.getByName(service.getType()).deploy(service);
	}

	public void update(ServiceDefinition service) throws ControllerException
	{

		controllersRegistry.getByName(service.getType()).update(service);
	}

	public ServiceDefinition getService(String name) throws ControllerException
	{
	
		for (ServiceControllerBaseInt controller : controllersRegistry.getAll())
		{
			ServiceDefinition service = controller.getService(name);
			if (service != null)
				return service;
		}
		return null;
	}

	public void remove(ServiceDefinition service) throws ControllerException
	{
		controllersRegistry.getByName(service.getType()).remove(service);
	}

	public MainServiceEditor getEditor(ServiceDefinition toEdit, ServiceEditorTab initTab, SubViewSwitcher subViewSwitcher) throws ControllerException
	{

		try
		{
			return new MainServiceEditor(msg, controllersRegistry, endpointMan.getEndpointTypes(), toEdit,
					initTab, subViewSwitcher);
		} catch (EngineException e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.createEditorError"), e);
		}
	}

	public void reload(ServiceDefinition service) throws ControllerException
	{
		controllersRegistry.getByName(service.getType()).reloadConfigFromFile(service);
	}
}
