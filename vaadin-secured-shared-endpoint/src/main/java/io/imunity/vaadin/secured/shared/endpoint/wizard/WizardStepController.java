/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.secured.shared.endpoint.wizard;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

class WizardStepController
{
	private final LinkedList<WizardStep> steps;
	private final List<WizardStepPreparer<?, ?>> wizardStepPreparers;
	private ListIterator<WizardStep> listIterator;
	private WizardStep current;

	WizardStepController(List<WizardStep> steps, List<WizardStepPreparer<?, ?>> wizardStepPreparers)
	{
		if(steps == null || steps.size() == 0)
			throw new IllegalArgumentException("You have to declare at least one step");
		this.steps = new LinkedList<>(steps);
		this.wizardStepPreparers = List.copyOf(wizardStepPreparers);
		startAgain();
	}

	void startAgain()
	{
		listIterator = steps.listIterator();
		current = listIterator.next();
	}

	WizardStep getNext()
	{
		WizardStep next = listIterator.next();
		prepareNextStep(current, next);
		current = next;
		current.initialize();
		return current;
	}

	WizardStep getPrev()
	{
		listIterator.previous();
		current = listIterator.previous();
		listIterator.next();
		current.initialize();
		return current;
	}

	WizardStep getCurrent()
	{
		return current;
	}

	boolean hasFinished()
	{
		if(listIterator.hasNext())
		{
			listIterator.next();
			boolean hasNext = listIterator.hasNext();
			listIterator.previous();
			return !hasNext;
		}
		return true;
	}

	boolean hasPrev()
	{
		if(listIterator.hasPrevious())
		{
			listIterator.previous();
			boolean hasPrevious = listIterator.hasPrevious();
			listIterator.next();
			return hasPrevious;
		}
		return false;
	}

	private void prepareNextStep(WizardStep currentStep, WizardStep nextStep)
	{
		wizardStepPreparers.stream()
				.filter(preparer -> preparer.isApplicable(currentStep, nextStep))
				.findFirst()
				.ifPresent(preparer -> preparer.prepare(currentStep, nextStep));
	}
}
