/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.console.views;

import io.imunity.vaadin.elements.NotificationPresenter;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.webui.console.services.ServiceControllerBaseInt;
import pl.edu.icm.unity.webui.console.services.ServiceControllersRegistry;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

import java.util.ArrayList;
import java.util.List;

@Controller
public class ServicesController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, ServicesController.class);


	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private final TypesRegistryBase<? extends ServiceControllerBaseInt> controllersRegistry;

	ServicesController(MessageSource msg, NotificationPresenter notificationPresenter,
					   ServiceControllersRegistry controllersRegistry)
	{
		this.msg = msg;
		this.controllersRegistry = controllersRegistry;
		this.notificationPresenter = notificationPresenter;
	}

	public List<ServiceDefinition> getServices()
	{
		List<ServiceDefinition> ret = new ArrayList<>();
		try
		{
			for (ServiceControllerBaseInt controller : controllersRegistry.getAll())
			{
				ret.addAll(controller.getServices());
			}
			return ret;
		}
		catch (ControllerException e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
			return List.of();
		}

	}

	public void undeploy(ServiceDefinition service)
	{
		try
		{
			controllersRegistry.getByName(service.getType()).undeploy(service);
		}
		catch (ControllerException e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
			log.error("Occurred while service was undeployed", e);
		}
	}

	public void deploy(ServiceDefinition service)
	{
		try
		{
			controllersRegistry.getByName(service.getType()).deploy(service);
		}
		catch (ControllerException e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
			log.error("Occurred while service was deployed", e);
		}
	}

	public void update(ServiceDefinition service)
	{
		try
		{
			controllersRegistry.getByName(service.getType()).update(service);
		} catch (ControllerException e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
			log.error("Occurred while service was updated", e);
		}
	}

	public ServiceDefinition getService(String name)
	{
		try
		{
			for (ServiceControllerBaseInt controller : controllersRegistry.getAll())
			{
				ServiceDefinition service;
					service = controller.getService(name);
				if (service != null)
					return service;
			}
		}
		catch (ControllerException e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
			log.error("Occurred while service was gotten", e);
		}
		return null;
	}

	public void remove(ServiceDefinition service)
	{
		try
		{
			controllersRegistry.getByName(service.getType()).remove(service);
		}
		catch (ControllerException e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
			log.error("Occurred while service was removed", e);
		}
	}

	public void reload(ServiceDefinition service)
	{
		try
		{
			controllersRegistry.getByName(service.getType()).reloadConfigFromFile(service);
		}
		catch (ControllerException e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
			log.error("Occurred while service was reload", e);
		}
	}
}
