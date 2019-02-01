/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.flows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.authentication.EndpointController;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Controller for all authentication flow view
 * @author P.Piernik
 *
 */
@Component
public class AuthenticationFlowsController
{
	private AuthenticationFlowManagement flowMan;
	private UnityMessageSource msg;
	private EndpointController endpointController;

	@Autowired
	public AuthenticationFlowsController(AuthenticationFlowManagement flowMan, UnityMessageSource msg,
			EndpointController endpointController)
	{
		this.flowMan = flowMan;
		this.msg = msg;
		this.endpointController = endpointController;
	}

	public boolean updateFlow(AuthenticationFlowDefinition flow) throws ControllerException

	{
		try
		{
			flowMan.updateAuthenticationFlow(flow);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AuthenticationFlowsController.updateError", flow.getName()),
					e.getMessage(), e);
		}

		return true;
	}

	public boolean removeFlow(AuthenticationFlowDefinition flow) throws ControllerException
	{
		try
		{
			flowMan.removeAuthenticationFlow(flow.getName());

		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AuthenticationFlowsController.removeError", flow.getName()),
					e.getMessage(), e);
		}

		return true;
	}

	public Collection<AuthenticationFlowEntry> getRealms() throws ControllerException
	{

		List<AuthenticationFlowEntry> ret = new ArrayList<>();
		Collection<AuthenticationFlowDefinition> flows;
		try
		{
			flows = flowMan.getAuthenticationFlows();
		} catch (EngineException e)
		{
			throw new ControllerException(msg.getMessage("AuthenticationFlowsController.getAllError"),
					e.getMessage(), e);
		}
		List<ResolvedEndpoint> endpoints =endpointController.getEndpoints();

		for (AuthenticationFlowDefinition flow : flows)
		{
			ret.add(new AuthenticationFlowEntry(flow, filterEndpoints(flow.getName(), endpoints)));
		}

		return ret;

	}

	public AuthenticationFlowEntry getFlow(String flowName) throws ControllerException
	{
		List<ResolvedEndpoint> endpoints = endpointController.getEndpoints();

		try
		{
			return new AuthenticationFlowEntry(flowMan.getAuthenticationFlow(flowName),
					filterEndpoints(flowName, endpoints));
		} catch (EngineException e)
		{
			throw new ControllerException(
					msg.getMessage("AuthenticationFlowsController.getError", flowName),
					e.getMessage(), e);
		}
	}

	private List<String> filterEndpoints(String flowName, List<ResolvedEndpoint> all)
	{
		return all.stream()
				.filter(e -> e.getEndpoint().getConfiguration().getAuthenticationOptions()
						.contains(flowName))
				.map(e -> e.getName()).sorted().collect(Collectors.toList());
	}

}
