/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.authenticators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.event.selection.SingleSelectionListener;

import io.imunity.webconsole.WebConsoleEndpointFactory;
import io.imunity.webconsole.common.EndpointController;
import io.imunity.webconsole.translationProfile.dryrun.DryRunWizardProvider;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationActionsRegistry;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.VaadinEndpoint;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactoriesRegistry;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Authenticators controller
 * 
 * @author P.Piernik
 *
 */
@Component
public class AuthenticatorsController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AuthenticatorsController.class);

	private AuthenticatorManagement authnMan;
	private AuthenticationFlowManagement flowsMan;
	private MessageSource msg;
	private EndpointController endpointController;
	private AuthenticatorEditorFactoriesRegistry editorsRegistry;
	private TranslationProfileManagement profileMan;
	private InputTranslationActionsRegistry inputActionsRegistry;

	@Autowired
	AuthenticatorsController(AuthenticatorManagement authnMan, MessageSource msg,
			EndpointController endpointController, AuthenticationFlowManagement flowsMan,
			AuthenticatorEditorFactoriesRegistry editorsRegistry, TranslationProfileManagement profileMan,
			InputTranslationActionsRegistry inputActionsRegistry)
	{
		this.authnMan = authnMan;
		this.msg = msg;
		this.endpointController = endpointController;
		this.flowsMan = flowsMan;
		this.editorsRegistry = editorsRegistry;
		this.profileMan = profileMan;
		this.inputActionsRegistry = inputActionsRegistry;
	}

	Collection<AuthenticatorEntry> getAllAuthenticators() throws ControllerException
	{

		List<AuthenticatorEntry> ret = new ArrayList<>();
		Collection<AuthenticatorInfo> authenticators;
		try
		{
			authenticators = authnMan.getAuthenticators(null);
		} catch (Exception e)
		{
			log.error("Can not get authenticators", e);
			throw new ControllerException(msg.getMessage("AuthenticatorsController.getAllError"), e);
		}

		Collection<AuthenticationFlowDefinition> flows = getFlows();
		List<ResolvedEndpoint> endpoints = endpointController.getEndpoints();

		for (AuthenticatorInfo auth : authenticators)
		{
			ret.add(new AuthenticatorEntry(new AuthenticatorDefinition(auth.getId(),
					auth.getTypeDescription().getVerificationMethod(), auth.getConfiguration(),
					auth.getLocalCredentialName().orElse(null)),
					filterEndpoints(auth.getId(), endpoints, flows)));
		}

		return ret;

	}

	void removeAuthenticator(AuthenticatorDefinition authneticator) throws ControllerException
	{
		try
		{
			authnMan.removeAuthenticator(authneticator.id);
		} catch (Exception e)
		{
			log.error("Can not remove authenticator", e);
			throw new ControllerException(
					msg.getMessage("AuthenticatorsController.removeError", authneticator.id), e);
		}
	}

	void addAuthenticator(AuthenticatorDefinition authenticator) throws ControllerException
	{
		try
		{

			authnMan.createAuthenticator(authenticator.id, authenticator.type, authenticator.configuration,
					authenticator.localCredentialName);
		} catch (Exception e)
		{
			log.error("Can not add authenticator", e);
			throw new ControllerException(
					msg.getMessage("AuthenticatorsController.addError", authenticator.id), e);
		}
	}

	void updateAuthenticator(AuthenticatorDefinition authenticator) throws ControllerException
	{
		try
		{
			authnMan.updateAuthenticator(authenticator.id, authenticator.configuration,
					authenticator.localCredentialName);
		} catch (Exception e)
		{
			log.error("Can not update authenticator", e);
			throw new ControllerException(
					msg.getMessage("AuthenticatorsController.updateError", authenticator.id), e);
		}
	}

	AuthenticatorEntry getAuthenticator(String id) throws ControllerException
	{
		Collection<AuthenticationFlowDefinition> flows = getFlows();
		List<ResolvedEndpoint> endpoints = endpointController.getEndpoints();

		try
		{
			AuthenticatorInfo authInfo = authnMan.getAuthenticator(id);

			return new AuthenticatorEntry(new AuthenticatorDefinition(authInfo.getId(),
					authInfo.getTypeDescription().getVerificationMethod(),
					authInfo.getConfiguration(), authInfo.getLocalCredentialName().orElse(null)),
					filterEndpoints(id, endpoints, flows));
		} catch (Exception e)
		{
			log.error("Can not get authenticator", e);
			throw new ControllerException(msg.getMessage("AuthenticatorsController.getError", id), e);
		}
	}

	private Collection<AuthenticationFlowDefinition> getFlows() throws ControllerException
	{
		try
		{
			return flowsMan.getAuthenticationFlows();
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AuthenticatorsController.getAllFlowsError"), e);
		}
	}

	MainAuthenticatorEditor getEditor(AuthenticatorEntry toEdit, SubViewSwitcher subViewSwitcher,
			SingleSelectionListener<AuthenticatorTypeDescription> typeChangeListener)
	{

		return new MainAuthenticatorEditor(msg, editorsRegistry, authnMan.getAvailableAuthenticatorsTypes(),
				toEdit, subViewSwitcher, Optional.ofNullable(typeChangeListener));
	}

	private List<String> filterEndpoints(String authneticator, List<ResolvedEndpoint> endpoints,
			Collection<AuthenticationFlowDefinition> flows)
	{
		Set<String> toSearch = new HashSet<>();
		toSearch.add(authneticator);
		flows.stream().filter(f -> f.getAllAuthenticators().contains(authneticator))
				.forEach(f -> toSearch.add(f.getName()));

		return endpoints.stream()
				.filter(e -> e.getEndpoint().getConfiguration().getAuthenticationOptions() != null && e.getEndpoint()
						.getConfiguration().getAuthenticationOptions().stream().anyMatch(toSearch::contains))
				.map(e -> e.getName()).sorted().collect(Collectors.toList());
	}

	private String getSandboxURL() throws ControllerException
	{
		List<ResolvedEndpoint> endpoints = endpointController.getEndpoints();
		for (ResolvedEndpoint endpoint : endpoints)
		{
			if (endpoint.getType().getName().equals(WebConsoleEndpointFactory.NAME))
			{
				return endpoint.getEndpoint().getContextAddress()
						+ VaadinEndpoint.SANDBOX_PATH_TRANSLATION;

			}
		}

		throw new ControllerException(msg.getMessage("AuthenticatorsController.getDryRunProviderError"), null);
	}

	public DryRunWizardProvider getDryRunProvider(SandboxAuthnRouter sandBoxRouter) throws ControllerException
	{
		try
		{
			DryRunWizardProvider provider = new DryRunWizardProvider(msg, getSandboxURL(), sandBoxRouter,
					profileMan, inputActionsRegistry);

			return provider;
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AuthenticatorsController.getDryRunProviderError"),
					e);
		}
	}

}
