/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.input_profiles.wizard;

import io.imunity.vaadin.endpoint.common.wizard.WizardStep;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnContext;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnEvent;

class ProfileStep extends WizardStep
{
	private ProfileStepComponent profileComponent;
	private SandboxAuthnContext ctx;
	
	ProfileStep(String label, ProfileStepComponent component)
	{
		super(label, component);
		this.profileComponent = component;
	}

	@Override
	protected void initialize()
	{
		profileComponent.handle(ctx.getRemotePrincipal().get().getAuthnInput());
		stepComplited();
		refreshWizard();
	}

	void prepareStep(SandboxAuthnEvent event)
	{
		this.ctx = event.ctx;
	}

}
