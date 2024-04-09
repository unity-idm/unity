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

import io.imunity.webconsole.common.EndpointController;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationPolicyConfigurationMapper;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Controller for all authentication flow views
 * 
 * @author P.Piernik
 *
 */
@Component
public class AuthenticationFlowsController
{
	private AuthenticationFlowManagement flowMan;
	private AuthenticatorManagement authMan;
	private MessageSource msg;
	private EndpointController endpointController;

	@Autowired
	AuthenticationFlowsController(AuthenticationFlowManagement flowMan, AuthenticatorManagement authMan, MessageSource msg,
			EndpointController endpointController)
	{
		this.flowMan = flowMan;
		this.authMan = authMan;
		this.msg = msg;
		this.endpointController = endpointController;
	}
	
	List<String> getAllAuthenticators() throws ControllerException
	{
		try
		{
			return authMan.getAuthenticators(null).stream().map(i -> i.getId())
					.collect(Collectors.toList());
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AuthenticationFlowsController.getAuthenticatorsError"), e);
		}
	}

	void addFlow(AuthenticationFlowDefinitionForBinder flow) throws ControllerException

	{
		try
		{
			flowMan.addAuthenticationFlow(map(flow));
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AuthenticationFlowsController.addError", flow.getName()), e);
		}
	}

	void updateFlow(AuthenticationFlowDefinitionForBinder flow) throws ControllerException

	{
		try
		{
			flowMan.updateAuthenticationFlow(map(flow));
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AuthenticationFlowsController.updateError", flow.getName()), e);
		}
	}

	void removeFlow(AuthenticationFlowDefinitionForBinder flow) throws ControllerException
	{
		try
		{
			flowMan.removeAuthenticationFlow(flow.getName());

		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AuthenticationFlowsController.removeError", flow.getName()), e);
		}
	}

	Collection<AuthenticationFlowEntry> getFlows() throws ControllerException
	{

		List<AuthenticationFlowEntry> ret = new ArrayList<>();
		Collection<AuthenticationFlowDefinition> flows;
		try
		{
			flows = flowMan.getAuthenticationFlows();
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AuthenticationFlowsController.getAllError"), e);
		}
		List<ResolvedEndpoint> endpoints = endpointController.getEndpoints();

		for (AuthenticationFlowDefinition flow : flows)
		{
			ret.add(new AuthenticationFlowEntry(map(flow), filterEndpoints(flow.getName(), endpoints)));
		}

		return ret;

	}

	AuthenticationFlowEntry getFlow(String flowName) throws ControllerException
	{
		List<ResolvedEndpoint> endpoints = endpointController.getEndpoints();

		try
		{
			return new AuthenticationFlowEntry(map(flowMan.getAuthenticationFlow(flowName)),
					filterEndpoints(flowName, endpoints));
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AuthenticationFlowsController.getError", flowName), e);
		}
	}

	private List<String> filterEndpoints(String flowName, List<ResolvedEndpoint> all)
	{
		return all.stream()
				.filter(e -> e.getEndpoint().getConfiguration().getAuthenticationOptions() != null
						&& e.getEndpoint().getConfiguration().getAuthenticationOptions().contains(flowName))
				.map(e -> e.getName()).sorted().collect(Collectors.toList());
	}
	
	private AuthenticationFlowDefinition map(AuthenticationFlowDefinitionForBinder flow)
	{
		return new AuthenticationFlowDefinition(flow.getName(), flow.getPolicy(), flow.getFirstFactorAuthenticators(),
				flow.getSecondFactorAuthenticators(),
				AuthenticationPolicyConfigurationMapper.map(flow.getPolicy(), flow.getPolicyConfiguration()));
	}

	private AuthenticationFlowDefinitionForBinder map(AuthenticationFlowDefinition flow)
	{
		return new AuthenticationFlowDefinitionForBinder(flow.getName(), flow.getPolicy(),
				flow.getFirstFactorAuthenticators(), flow.getSecondFactorAuthenticators(),
				AuthenticationPolicyConfigurationMapper.map(flow.getPolicyConfiguration()));
	}

}
