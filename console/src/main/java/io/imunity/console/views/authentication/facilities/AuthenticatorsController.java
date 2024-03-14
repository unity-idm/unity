/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.facilities;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.VaadinServlet;
import io.imunity.console.views.sandbox.SandboxView;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.wizard.Wizard;
import io.imunity.vaadin.elements.wizard.WizardStepPreparer;
import io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext;
import io.imunity.vaadin.endpoint.common.sandbox.SandboxAuthnLaunchStep;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInfo;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationActionsRegistry;
import io.imunity.vaadin.endpoint.common.exceptions.ControllerException;

import java.util.*;
import java.util.stream.Collectors;


@Component
public class AuthenticatorsController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AuthenticatorsController.class);

	private final AuthenticatorManagement authnMan;
	private final AuthenticationFlowManagement flowsMan;
	private final MessageSource msg;
	private final EndpointManagement endpointMan;
	private final TranslationProfileManagement profileMan;
	private final InputTranslationActionsRegistry inputActionsRegistry;
	private final NotificationPresenter notificationPresenter;

	AuthenticatorsController(AuthenticatorManagement authnMan, MessageSource msg,
			EndpointManagement endpointMan, AuthenticationFlowManagement flowsMan,
			TranslationProfileManagement profileMan,
			InputTranslationActionsRegistry inputActionsRegistry, NotificationPresenter notificationPresenter)
	{
		this.authnMan = authnMan;
		this.msg = msg;
		this.endpointMan = endpointMan;
		this.flowsMan = flowsMan;
		this.profileMan = profileMan;
		this.inputActionsRegistry = inputActionsRegistry;
		this.notificationPresenter = notificationPresenter;
	}

	Collection<AuthenticatorEntry> getAllAuthenticators()
	{

		List<AuthenticatorEntry> ret = new ArrayList<>();
		Collection<AuthenticatorInfo> authenticators = List.of();
		try
		{
			authenticators = authnMan.getAuthenticators(null);
		} catch (Exception e)
		{
			log.error("Can not get authenticators", e);
			notificationPresenter.showError(msg.getMessage("AuthenticatorsController.getAllError"), e.getMessage());
		}

		Collection<AuthenticationFlowDefinition> flows = getFlows();
		List<ResolvedEndpoint> endpoints = getEndpoints();

		for (AuthenticatorInfo auth : authenticators)
		{
			ret.add(new AuthenticatorEntry(new AuthenticatorDefinition(auth.getId(),
					auth.getTypeDescription().getVerificationMethod(), auth.getConfiguration(),
					auth.getLocalCredentialName().orElse(null)),
					filterEndpoints(auth.getId(), endpoints, flows)));
		}

		return ret;

	}

	void removeAuthenticator(AuthenticatorDefinition authneticator)
	{
		try
		{
			authnMan.removeAuthenticator(authneticator.id);
		} catch (Exception e)
		{
			log.error("Can not remove authenticator", e);
			notificationPresenter.showError(
					msg.getMessage("AuthenticatorsController.removeError", authneticator.id), e.getMessage());
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
			throw new ControllerException(msg.getMessage("AuthenticatorsController.addError", authenticator.id), e);
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
			throw new ControllerException(msg.getMessage("AuthenticatorsController.updateError", authenticator.id), e);
		}
	}

	AuthenticatorEntry getAuthenticator(String id)
	{
		Collection<AuthenticationFlowDefinition> flows = getFlows();
		List<ResolvedEndpoint> endpoints = getEndpoints();

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
			notificationPresenter.showError(msg.getMessage("AuthenticatorsController.getError", id), e.getMessage());
		}
		return null;
	}

	private Collection<AuthenticationFlowDefinition> getFlows()
	{
		try
		{
			return flowsMan.getAuthenticationFlows();
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AuthenticatorsController.getAllFlowsError"), e.getMessage());
		}
		return List.of();
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
				.map(ResolvedEndpoint::getName).sorted().collect(Collectors.toList());
	}

	public Dialog getWizard()
	{
		String contextPath = VaadinServlet.getCurrent()
				.getServletConfig()
				.getServletContext()
				.getContextPath();
		Runnable sandBoxNewPageOpener = () -> UI.getCurrent()
				.getPage()
				.executeJs("window.open('" + contextPath + SandboxView.SANDBOX_PATH
						+ "/', '_blank', 'resizable,status=0,location=0')");
		SandboxAuthnRouter router = Vaadin2XWebAppContext.getCurrentWebAppSandboxAuthnRouter();
		Map<String, TranslationProfile> inputProfiles;
		try
		{
			inputProfiles = profileMan.listInputProfiles();
		}
		catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
			log.error(e);
			return new Dialog(msg.getMessage("error"));
		}
		Wizard wizard = Wizard.builder()
				.addStep(new IntroStep(msg))
				.addStep(new SandboxAuthnLaunchStep(msg.getMessage("Wizard.SandboxStep.caption"),
						new VerticalLayout(new Span(msg.getMessage("Wizard.SandboxStepComponent.infoLabel")),
								new Button(msg.getMessage("Wizard.SandboxStepComponent.sboxButton"),
										e -> sandBoxNewPageOpener.run())),
						router, sandBoxNewPageOpener))
				.addNextStepPreparer(new WizardStepPreparer<>(SandboxAuthnLaunchStep.class, DryRunStep.class,
						(step1, step2) -> step2.prepareStep(step1.event)))
				.addStep(new DryRunStep(msg, inputProfiles, inputActionsRegistry))
				.addStep(new FinishStep(null, new Span()))
				.addMessageSource(msg::getMessage)
				.build();
		wizard.setHeight("80%");
		return wizard;
	}

	public List<ResolvedEndpoint> getEndpoints()
	{
		try
		{
			return endpointMan.getDeployedEndpoints();
		} catch (EngineException e)
		{
			notificationPresenter.showError(
					msg.getMessage("EndpointController.getAllError"), e.getMessage());
		}
		return List.of();
	}

}
