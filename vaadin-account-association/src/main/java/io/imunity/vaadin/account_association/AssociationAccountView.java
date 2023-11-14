/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.account_association;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServlet;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.wizard.Wizard;
import io.imunity.vaadin.elements.wizard.WizardStepPreparer;
import io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext;
import io.imunity.vaadin.endpoint.common.sandbox.SandboxAuthnLaunchStep;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;

import java.util.Map;

import static pl.edu.icm.unity.webui.VaadinEndpoint.SANDBOX_PATH_ASSOCIATION;

@PermitAll
@Route("/sec/account-association")
class AssociationAccountView extends Composite<VerticalLayout> implements HasDynamicTitle
{
	private final MessageSource msg;
	private final InputTranslationEngine inputTranslationEngine;
	private final NotificationPresenter notificationPresenter;
	AssociationAccountView(MessageSource msg, InputTranslationEngine inputTranslationEngine, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.inputTranslationEngine = inputTranslationEngine;
		this.notificationPresenter = notificationPresenter;
		runWizard();
	}

	void runWizard()
	{
		String contextPath = VaadinServlet.getCurrent().getServletConfig().getServletContext().getContextPath();
		Runnable sandBoxNewPageOpener = () -> UI.getCurrent().getPage()
				.executeJs("window.open('"+ contextPath + SANDBOX_PATH_ASSOCIATION + "/', '_blank', 'resizable,status=0,location=0')");
		SandboxAuthnRouter router = Vaadin2XWebAppContext.getCurrentWebAppSandboxAuthnRouter();

		Runnable finishTask = () ->	UI.getCurrent().navigate(StatusView.class, QueryParameters.simple(Map.of(
				StatusView.TITLE_PARAM, msg.getMessage("ConnectId.ConfirmStep.mergeSuccessfulCaption"),
				StatusView.DESCRIPTION_PARAM, msg.getMessage("ConnectId.ConfirmStep.mergeSuccessful"))
		));

		Wizard wizard = Wizard.builder()
				.addStep(new IntroStep(msg))
				.addStep(new SandboxAuthnLaunchStep(
						msg.getMessage("Wizard.SandboxStep.caption"),
						new VerticalLayout(
								new Span(msg.getMessage("ConnectId.introLabel")),
								new Button(msg.getMessage("Wizard.SandboxStepComponent.sboxButton"),
										e -> sandBoxNewPageOpener.run())
						),
						router,
						sandBoxNewPageOpener)
				)
				.addNextStepPreparer(new WizardStepPreparer<>(
						SandboxAuthnLaunchStep.class,
						MergingUserConfirmationStep.class,
						(step1, step2) -> step2.prepareStep(step1.event, step1.sessionEntityId))
				)
				.addStep(new MergingUserConfirmationStep(msg.getMessage("ConnectId.ConfirmStep.caption"), msg, inputTranslationEngine))
				.addNextStepPreparer(new WizardStepPreparer<>(
						MergingUserConfirmationStep.class,
						FinalConnectIdStep.class,
						(step1, step2) -> step2.prepareStep(step1.ctx.getRemotePrincipal().orElse(null)))
				)
				.addStep(new FinalConnectIdStep(null, new VerticalLayout(), inputTranslationEngine, notificationPresenter, msg, finishTask))
				.addMessageSource(m -> msg.getMessage(m))
				.addCancelTask(() -> UI.getCurrent().navigate(StatusView.class, QueryParameters.of(StatusView.TITLE_PARAM, msg.getMessage("Wizard.canceled"))))
				.title(msg.getMessage("ConnectId.wizardCaption"))
				.build();
		getContent().add(wizard);
	}

	@Override
	public String getPageTitle()
	{
		return msg.getMessage("AssociationAccount.title");
	}
}
