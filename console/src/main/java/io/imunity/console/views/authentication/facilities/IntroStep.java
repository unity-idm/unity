/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.facilities;

import com.vaadin.flow.component.html.Span;
import io.imunity.vaadin.elements.wizard.WizardStep;
import pl.edu.icm.unity.base.message.MessageSource;

class IntroStep extends WizardStep
{
	IntroStep(MessageSource msg)
	{
		super(msg.getMessage("Wizard.IntroStep.caption"), new Span(msg.getMessage("DryRun.IntroStepComponent.introLabel")));
	}

	@Override
	protected void initialize()
	{
		stepComplited();
		refreshWizard();
	}
}


