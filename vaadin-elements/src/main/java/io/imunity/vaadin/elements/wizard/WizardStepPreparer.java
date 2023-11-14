/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements.wizard;

import java.util.function.BiConsumer;

public class WizardStepPreparer<WS1 extends WizardStep, WS2 extends WizardStep>
{
	private final Class<WS1> currentStepClass;
	private final Class<WS2> nextStepClass;
	final BiConsumer<WS1, WS2> preparer;

	public WizardStepPreparer(Class<WS1> currentStepClass, Class<WS2> nextStepClass, BiConsumer<WS1, WS2> preparer)
	{
		this.currentStepClass = currentStepClass;
		this.nextStepClass = nextStepClass;
		this.preparer = preparer;
	}

	boolean isApplicable(WizardStep currentStep, WizardStep nextStep)
	{
		return currentStep.getClass().isAssignableFrom(currentStepClass) && nextStep.getClass().isAssignableFrom(nextStepClass);
	}

	@SuppressWarnings("unchecked")
	void prepare(WizardStep currentStep, WizardStep nextStep)
	{
		preparer.accept((WS1)currentStep, (WS2)nextStep);
	}
}
