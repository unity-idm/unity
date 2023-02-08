/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.secured.shared.endpoint.wizard;

import java.util.function.BiConsumer;

public final class WizardStepController<WS1 extends WizardStep, WS2 extends WizardStep>
{
	private final WS1 wizardStartStep;
	private final WS2 wizardFinalStep;
	private final BiConsumer<WS1, WS2> roadPreparer;
	private final boolean imminentlyGo;
	private Wizard wizard;

	public WizardStepController(WS1 wizardStartStep, WS2 wizardFinalStep, BiConsumer<WS1, WS2> roadPreparer,
	                             boolean imminentlyGo)
	{
		this.wizardStartStep = wizardStartStep;
		this.wizardFinalStep = wizardFinalStep;
		this.roadPreparer = roadPreparer;
		this.imminentlyGo = imminentlyGo;
	}

	public WizardStepController(WS1 wizardStartStep, WS2 wizardFinalStep, BiConsumer<WS1, WS2> roadPreparer)
	{
		this(wizardStartStep, wizardFinalStep, roadPreparer, false);
	}

	public WizardStepController(WS1 wizardStartStep, WS2 wizardFinalStep)
	{
		this(wizardStartStep, wizardFinalStep, (x, y) -> {}, false);
	}

	WizardStep getCurrentStep()
	{
		return wizardStartStep;
	}

	WizardStep getEndStep()
	{
		return wizardFinalStep;
	}

	void setWizard(Wizard wizard)
	{
		this.wizard = wizard;
	}

	void startStepCompleted()
	{
		roadPreparer.accept(wizardStartStep, wizardFinalStep);
		if(imminentlyGo)
			wizard.go();
		else
			wizard.readyToGo();
	}
}
