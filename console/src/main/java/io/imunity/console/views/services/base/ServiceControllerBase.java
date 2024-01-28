/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.console.views.services.base;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.endpoint.common.api.services.ServiceControllerBaseInt;
import io.imunity.vaadin.endpoint.common.api.services.ServiceDefinition;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditorComponent.ServiceEditorTab;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Common part for idp and standard service controllers
 * @author P.Piernik
 *
 */
public class ServiceControllerBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, ServiceControllerBase.class);

	
	private final MessageSource msg;
	private final EndpointManagement endpointMan;
	private final TypesRegistryBase<? extends ServiceControllerBaseInt> controllersRegistry;
	private final NotificationPresenter notificationPresenter;

	public ServiceControllerBase(MessageSource msg, EndpointManagement endpointMan,
			TypesRegistryBase< ? extends ServiceControllerBaseInt> controllersRegistry, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.endpointMan = endpointMan;
		this.controllersRegistry = controllersRegistry;
		this.notificationPresenter = notificationPresenter;
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
					initTab, subViewSwitcher, notificationPresenter);
		} catch (EngineException e)
		{
			log.error("Can not create service editor", e);
			throw new ControllerException(msg.getMessage("ServicesController.createEditorError"), e);
		}
	}

	public void reload(ServiceDefinition service) throws ControllerException
	{
		controllersRegistry.getByName(service.getType()).reloadConfigFromFile(service);
	}
}
