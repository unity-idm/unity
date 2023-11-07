/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.input_profiles.wizard;

import com.vaadin.flow.component.html.Span;

import io.imunity.vaadin.endpoint.common.wizard.WizardStep;
import pl.edu.icm.unity.base.message.MessageSource;

class IntroStep extends WizardStep
{
	IntroStep(MessageSource msg)
	{
		super(msg.getMessage("Wizard.IntroStep.caption"), getContent(msg.getMessage("Wizard.IntroStepComponent.introLabel")));
	}

	@Override
	protected void initialize()
	{
		stepComplited();
		refreshWizard();
	}
	private static Span getContent(String title)
	{
		Span span = new Span();
		span.getElement().setProperty("innerHTML", title);
		return span;
		
	}

}


