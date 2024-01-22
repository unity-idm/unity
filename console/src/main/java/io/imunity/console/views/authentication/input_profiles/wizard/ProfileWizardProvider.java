/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.input_profiles.wizard;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.VaadinServlet;
import io.imunity.console.views.sandbox.SandboxView;
import io.imunity.console.tprofile.TranslationProfileEditor;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.wizard.Wizard;
import io.imunity.vaadin.elements.wizard.WizardStepPreparer;
import io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext;
import io.imunity.vaadin.endpoint.common.sandbox.SandboxAuthnLaunchStep;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;

import java.util.function.Consumer;

@org.springframework.stereotype.Component
public class ProfileWizardProvider
{

	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	
	ProfileWizardProvider(MessageSource msg, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	public Wizard getWizard(TranslationProfileEditor editor, Runnable closeWizard,
			Consumer<TranslationProfile> finish)
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

		Wizard wizard = Wizard.builder()
				.addStep(new IntroStep(msg))
				.addStep(new SandboxAuthnLaunchStep(msg.getMessage("Wizard.SandboxStep.caption"),
						new VerticalLayout(new Span(msg.getMessage("Wizard.SandboxStepComponent.infoLabel")),
								new Button(msg.getMessage("Wizard.SandboxStepComponent.sboxButton"),
										e -> sandBoxNewPageOpener.run())),
						router, sandBoxNewPageOpener))
				.addNextStepPreparer(new WizardStepPreparer<>(SandboxAuthnLaunchStep.class, ProfileStep.class,
						(step1, step2) -> step2.prepareStep(step1.event)))
				.addStep(new ProfileStep(msg.getMessage("Wizard.ProfileStep.caption"),
						new ProfileStepComponent(msg, editor), notificationPresenter, msg))
				.addStep(new AddProfileStep(null, new VerticalLayout(), editor, finish, notificationPresenter, msg))
				.addMessageSource(msg::getMessage)
				.addCancelTask(closeWizard)
				.build();
		wizard.setWidth(110, Unit.EM);
		wizard.setHeight(70, Unit.EM);
		
		return wizard;
	}
}
