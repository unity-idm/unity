/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.input_profiles.wizard;

import static pl.edu.icm.unity.webui.VaadinEndpoint.SANDBOX_PATH_ASSOCIATION;

import java.util.function.Consumer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.VaadinServlet;

import io.imunity.console.tprofile.TranslationProfileEditor;
import io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext;
import io.imunity.vaadin.endpoint.common.sandbox.SandboxAuthnLaunchStep;
import io.imunity.vaadin.endpoint.common.wizard.Wizard;
import io.imunity.vaadin.endpoint.common.wizard.WizardStepPreparer;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;

@org.springframework.stereotype.Component
public class ProfileWizardProvider
{

	private final MessageSource msg;

	ProfileWizardProvider(MessageSource msg)
	{
		this.msg = msg;
	}

	public Component getWizard(TranslationProfileEditor editor, Runnable closeWizard,
			Consumer<TranslationProfile> finish)
	{
		String contextPath = VaadinServlet.getCurrent()
				.getServletConfig()
				.getServletContext()
				.getContextPath();
		Runnable sandBoxNewPageOpener = () -> UI.getCurrent()
				.getPage()
				.executeJs("window.open('" + contextPath + SANDBOX_PATH_ASSOCIATION
						+ "/', '_blank', 'resizable,status=0,location=0')");
		SandboxAuthnRouter router = Vaadin2XWebAppContext.getCurrentWebAppSandboxAuthnRouter();

		return Wizard.builder()
				.addStep(new IntroStep(msg))
				.addStep(new SandboxAuthnLaunchStep(msg.getMessage("Wizard.SandboxStep.caption"),
						new VerticalLayout(new Span(msg.getMessage("ConnectId.introLabel")),
								new Button(msg.getMessage("Wizard.SandboxStepComponent.sboxButton"),
										e -> sandBoxNewPageOpener.run())),
						router, sandBoxNewPageOpener))
				.addNextStepPreparer(new WizardStepPreparer<>(SandboxAuthnLaunchStep.class, ProfileStep.class,
						(step1, step2) -> step2.prepareStep(step1.event)))
				.addStep(new ProfileStep(msg.getMessage("Wizard.ProfileStep.caption"),
						new ProfileStepComponent(msg, editor)))
				.addStep(new AddProfileStep(null, new VerticalLayout(), editor, finish))
				.addMessageSource(msg)
				.addCancelTask(closeWizard)
				.build();
	}
}
