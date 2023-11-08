/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.input_profiles.wizard;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.wizard.WizardStep;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnContext;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnEvent;

class ProfileStep extends WizardStep
{
	private final ProfileStepComponent profileComponent;
	private final NotificationPresenter notificationPresenter;
	private final MessageSource msg;
	private SandboxAuthnContext ctx;

	ProfileStep(String label, ProfileStepComponent component, NotificationPresenter notificationPresenter,
			MessageSource msg)
	{
		super(label, component);
		this.profileComponent = component;
		this.notificationPresenter = notificationPresenter;
		this.msg = msg;
	}

	@Override
	protected void initialize()
	{
		if (ctx.getRemotePrincipal()
				.isEmpty())
		{
			stepRequiredPrevStep();
			refreshWizard();
			notificationPresenter.showError(msg.getMessage("Wizard.ProfileStep.noRemoteData"), "");
			return;
		}

		profileComponent.handle(ctx.getRemotePrincipal().get().getAuthnInput());
		stepComplited();
		refreshWizard();
	}

	void prepareStep(SandboxAuthnEvent event)
	{
		this.ctx = event.ctx;
	}

}
