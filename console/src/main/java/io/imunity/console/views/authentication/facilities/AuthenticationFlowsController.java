/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.facilities;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.vaadin.elements.NotificationPresenter;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationPolicyConfigurationMapper;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInfo;

/**
 * Controller for all authentication flow views
 */
@Component
public class AuthenticationFlowsController
{
	private final AuthenticationFlowManagement flowMan;
	private final AuthenticatorManagement authMan;
	private final MessageSource msg;
	private final EndpointManagement endpointMan;
	private final NotificationPresenter notificationPresenter;

	@Autowired
	AuthenticationFlowsController(AuthenticationFlowManagement flowMan, AuthenticatorManagement authMan,
			MessageSource msg, EndpointManagement endpointMan, NotificationPresenter notificationPresenter)
	{
		this.flowMan = flowMan;
		this.authMan = authMan;
		this.msg = msg;
		this.endpointMan = endpointMan;
		this.notificationPresenter = notificationPresenter;
	}

	List<String> getAllAuthenticators()
	{
		try
		{
			return authMan.getAuthenticators(null)
					.stream()
					.map(AuthenticatorInfo::getId)
					.collect(Collectors.toList());
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AuthenticationFlowsController.getAuthenticatorsError"),
					e.getMessage());
		}
		return List.of();
	}

	void addFlow(AuthenticationFlowDefinitionForBinder flow)
	{
		try
		{
			flowMan.addAuthenticationFlow(map(flow));
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AuthenticationFlowsController.addError", flow.getName()),
					e.getMessage());
		}
	}

	void updateFlow(AuthenticationFlowDefinitionForBinder flow)

	{
		try
		{
			flowMan.updateAuthenticationFlow(map(flow));
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AuthenticationFlowsController.updateError", flow.getName()),
					e.getMessage());
		}
	}

	void removeFlow(AuthenticationFlowDefinitionForBinder flow)
	{
		try
		{
			flowMan.removeAuthenticationFlow(flow.getName());
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AuthenticationFlowsController.removeError", flow.getName()),
					e.getMessage());
		}
	}

	Collection<AuthenticationFlowEntry> getFlows()
	{
		Collection<AuthenticationFlowDefinition> flows;
		try
		{
			flows = flowMan.getAuthenticationFlows();
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AuthenticationFlowsController.getAllError"),
					e.getMessage());
			return List.of();
		}
		List<ResolvedEndpoint> endpoints = getEndpoints();

		return flows.stream()
				.map(flow -> new AuthenticationFlowEntry(map(flow), filterEndpoints(flow.getName(), endpoints)))
				.collect(Collectors.toList());
	}

	AuthenticationFlowEntry getFlow(String flowName)
	{
		List<ResolvedEndpoint> endpoints = getEndpoints();

		try
		{
			return new AuthenticationFlowEntry(map(flowMan.getAuthenticationFlow(flowName)),
					filterEndpoints(flowName, endpoints));
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AuthenticationFlowsController.getError", flowName),
					e.getMessage());
		}
		return null;
	}

	List<ResolvedEndpoint> getEndpoints()
	{
		try
		{
			return endpointMan.getDeployedEndpoints();
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("EndpointController.getAllError"), e.getMessage());
		}
		return List.of();
	}

	private List<String> filterEndpoints(String flowName, List<ResolvedEndpoint> all)
	{
		return all.stream()
				.filter(e -> e.getEndpoint()
						.getConfiguration()
						.getAuthenticationOptions() != null && e.getEndpoint()
								.getConfiguration()
								.getAuthenticationOptions()
								.contains(flowName))
				.map(ResolvedEndpoint::getName)
				.sorted()
				.collect(Collectors.toList());
	}

	AuthenticationFlowDefinition map(AuthenticationFlowDefinitionForBinder flow)
	{
		return new AuthenticationFlowDefinition(flow.getName(), flow.getPolicy(), flow.getFirstFactorAuthenticators(),
				flow.getSecondFactorAuthenticators(),
				AuthenticationPolicyConfigurationMapper.map(flow.getPolicy(), flow.getPolicyConfiguration()));
	}

	AuthenticationFlowDefinitionForBinder map(AuthenticationFlowDefinition flow)
	{
		return new AuthenticationFlowDefinitionForBinder(flow.getName(), flow.getPolicy(),
				flow.getFirstFactorAuthenticators(), flow.getSecondFactorAuthenticators(),
				AuthenticationPolicyConfigurationMapper.map(flow.getPolicyConfiguration()));
	}

}
