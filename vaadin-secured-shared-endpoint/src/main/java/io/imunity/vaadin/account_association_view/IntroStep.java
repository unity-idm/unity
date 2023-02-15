/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.account_association_view;

import com.vaadin.flow.component.html.Label;
import io.imunity.vaadin.account_association_view.wizard.WizardStep;
import pl.edu.icm.unity.MessageSource;

class IntroStep extends WizardStep
{
	public IntroStep(MessageSource msg)
	{
		super(msg.getMessage("Wizard.IntroStep.caption"), new Label(msg.getMessage("ConnectId.introLabel")));
	}

	@Override
	protected void initialize()
	{
		stepComplited();
		refreshWizard();
	}
}
