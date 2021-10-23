/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Base service controller interface. 
 * 
 * @author P.Piernik
 *
 */
public interface ServiceControllerBaseInt
{
	List<ServiceDefinition> getServices() throws ControllerException;
	ServiceDefinition getService(String name) throws ControllerException;
	void deploy(ServiceDefinition service) throws ControllerException;
	void undeploy(ServiceDefinition service) throws ControllerException;
	void update(ServiceDefinition service) throws ControllerException;
	void remove(ServiceDefinition service) throws ControllerException;
	String getSupportedEndpointType();
	ServiceEditor getEditor(SubViewSwitcher subViewSwitcher) throws EngineException;
	void reloadConfigFromFile(ServiceDefinition service) throws ControllerException;
}
