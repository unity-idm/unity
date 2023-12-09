/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.facilities;

import com.vaadin.flow.component.Component;
import io.imunity.vaadin.elements.wizard.WizardStep;

class FinishStep extends WizardStep
{

	public FinishStep(String label, Component component)
	{
		super(label, component);
	}

	@Override
	protected void initialize()
	{
		wizard.close();;
	}
}
