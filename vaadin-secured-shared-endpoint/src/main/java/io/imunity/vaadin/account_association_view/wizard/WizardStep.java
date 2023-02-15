/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.account_association_view.wizard;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;

@Tag("div")
public abstract class WizardStep extends Component implements HasComponents
{
	protected final String label;
	protected final Component component;
	private Wizard wizard;
	private WizardStepStatus status = WizardStepStatus.IN_PROGRESS;

	public WizardStep(String label, Component component)
	{
		this.label = label;
		this.component = component;
		add(component);
	}

	void setWizard(Wizard wizard)
	{
		this.wizard = wizard;
	}

	WizardStepStatus getStatus()
	{
		return status;
	}

	protected final void refreshWizard()
	{
		wizard.refresh();
	}

	protected final void stepComplited()
	{
		status = WizardStepStatus.COMPLITED;
	}

	protected final void stepInProgress()
	{
		status = WizardStepStatus.IN_PROGRESS;
	}

	protected final void stepRequiredNewStep()
	{
		status = WizardStepStatus.NEXT_STEP_REQUIRED;
	}

	protected abstract void initialize();
}
