/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.facilities;

import com.vaadin.flow.component.Component;
import io.imunity.vaadin.elements.wizard.WizardStep;

class FinishStep extends WizardStep
{
	private final Runnable finishTask;

	public FinishStep(String label, Component component, Runnable finishTask)
	{
		super(label, component);
		this.finishTask = finishTask;
	}

	@Override
	protected void initialize()
	{
		finishTask.run();
	}
}
