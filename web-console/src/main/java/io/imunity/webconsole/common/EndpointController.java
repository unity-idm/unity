/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole.common;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.webui.exceptions.ControllerException;
/**
 * 
 * @author P.Piernik
 *
 */
@Component
public class EndpointController
{
	private EndpointManagement endpointMan;
	private MessageSource msg;
	
	@Autowired
	public EndpointController(EndpointManagement endpointMan, MessageSource msg)
	{
		this.endpointMan = endpointMan;
		this.msg = msg;
	}

	public List<ResolvedEndpoint> getEndpoints() throws ControllerException
	{
		List<ResolvedEndpoint> endpoints;
		try
		{
			endpoints = endpointMan.getDeployedEndpoints();
		} catch (EngineException e)
		{
			throw new ControllerException(
					msg.getMessage("EndpointController.getAllError"),
					e.getMessage(), e);
		}
		return endpoints;
	}
}
