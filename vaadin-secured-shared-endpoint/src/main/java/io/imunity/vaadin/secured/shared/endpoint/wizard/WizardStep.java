/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.secured.shared.endpoint.wizard;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;

@Tag("div")
public class WizardStep extends Component implements HasComponents
{
	private WizardStepController<?,?> wizardStepController;
	protected final String label;
	protected final Component component;

	public WizardStep(String label, Component component)
	{
		this.label = label;
		this.component = component;
		add(component);
	}

	public void setWizardStepController(WizardStepController<?, ?> wizardStepController)
	{
		this.wizardStepController = wizardStepController;
	}

	protected final void completed()
	{
		wizardStepController.startStepCompleted();
	}

	protected void run()
	{
		completed();
	}
}
