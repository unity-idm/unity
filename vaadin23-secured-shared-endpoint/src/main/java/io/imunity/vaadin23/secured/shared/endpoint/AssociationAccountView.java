/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.secured.shared.endpoint;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServlet;
import io.imunity.vaadin23.elements.NotificationPresenter;
import io.imunity.vaadin23.secured.shared.endpoint.wizard.Wizard;
import io.imunity.vaadin23.secured.shared.endpoint.wizard.WizardStep;
import io.imunity.vaadin23.secured.shared.endpoint.wizard.WizardStepController;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;

import static io.imunity.vaadin23.secured.shared.endpoint.Vaadin23WebAppContextWithSandbox.getCurrentWebAppSandboxAuthnRouter;
import static pl.edu.icm.unity.webui.VaadinEndpoint.SANDBOX_PATH_ASSOCIATION;

@Route("/sec/account-association")
class AssociationAccountView extends Composite<VerticalLayout>
{
	private final MessageSource msg;
	private final InputTranslationEngine inputTranslationEngine;
	private final NotificationPresenter notificationPresenter;
	public AssociationAccountView(MessageSource msg, InputTranslationEngine inputTranslationEngine, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.inputTranslationEngine = inputTranslationEngine;
		this.notificationPresenter = notificationPresenter;
		runWizard();
	}

	void runWizard()
	{
		String contextPath = VaadinServlet.getCurrent().getServletConfig().getServletContext().getContextPath();
		Runnable sandBoxNewPageOpener = () -> UI.getCurrent().getPage().executeJs("window.open('"+ contextPath + SANDBOX_PATH_ASSOCIATION + "/', '_blank', 'resizable,status=0,location=0')");
		WizardStep introStep = new WizardStep(msg.getMessage("Wizard.IntroStep.caption"), new Label(msg.getMessage("ConnectId.introLabel")));
		SandboxAuthnRouter router = getCurrentWebAppSandboxAuthnRouter();
		SandboxStep sandboxStep = new SandboxStep(msg.getMessage("Wizard.SandboxStep.caption"), new VerticalLayout(new Label(msg.getMessage("ConnectId.introLabel")), new Button(msg.getMessage("Wizard.SandboxStepComponent.sboxButton"), e -> sandBoxNewPageOpener.run())), router, sandBoxNewPageOpener);
		MergingUserConfirmationStep mergingUserConfirmationStep = new MergingUserConfirmationStep(msg.getMessage("ConnectId.ConfirmStep.caption"), msg, inputTranslationEngine);
		FinalStep finalStep = new FinalStep(null, new VerticalLayout(), inputTranslationEngine, notificationPresenter, msg);

		Wizard wizard = Wizard.builder()
				.addStepControllers(new WizardStepController<>(
						introStep, sandboxStep
				))
				.addStepControllers(new WizardStepController<>(
						sandboxStep, mergingUserConfirmationStep,
						(SandboxStep step1, MergingUserConfirmationStep step2) -> step2.prepareStep(step1.event),
						 true
				))
				.addStepControllers(new WizardStepController<>(
						mergingUserConfirmationStep, finalStep,
						(MergingUserConfirmationStep step1, FinalStep step2) -> step2.prepareStep(step1.ctx.getRemotePrincipal().orElse(null))
				))
				.addMessageSource(msg)
				.addCancelTask(() -> UI.getCurrent().navigate(StatusView.class, QueryParameters.of("info", msg.getMessage("Wizard.canceled"))))
				.title(msg.getMessage("ConnectId.wizardCaption"))
				.build();
		getContent().add(wizard);
	}

}
