/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.account_association;

import com.vaadin.flow.component.html.Span;
import io.imunity.vaadin.account_association.wizard.WizardStep;
import pl.edu.icm.unity.base.message.MessageSource;

class IntroStep extends WizardStep
{
	public IntroStep(MessageSource msg)
	{
		super(msg.getMessage("Wizard.IntroStep.caption"), new Span(msg.getMessage("ConnectId.introLabel")));
	}

	@Override
	protected void initialize()
	{
		stepComplited();
		refreshWizard();
	}
}
