/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.secured.shared.endpoint.wizard;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import pl.edu.icm.unity.MessageSource;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class Wizard extends VerticalLayout
{
	private final Button finishButton = new Button();
	private final Button nextButton = new Button();
	private final Button backButton = new Button();
	private final ProgressBar progressBar = new ProgressBar();
	private final double progressBarIncrementer;
	private final VerticalLayout contentLayout = new VerticalLayout();
	private final UI ui = UI.getCurrent();
	private final LinkedList<WizardStepController<?, ?>> steps;
	private ListIterator<WizardStepController<?, ?>> listIterator;


	public Wizard(List<WizardStepController<?, ?>> steps, MessageSource msg, Runnable cancelTask, String title)
	{
		if(steps == null || steps.size() == 0)
			throw new IllegalArgumentException("You have to declare at least one step");
		this.steps = new LinkedList<>(steps);
		this.progressBarIncrementer = 1.0/(steps.size());

		H2 titleComponent = new H2(title);
		titleComponent.getStyle().set("margin-top", "0");
		HorizontalLayout labelsLayout = new HorizontalLayout();
		HorizontalLayout buttonsLayout = new HorizontalLayout();

		initLabelsLayout(steps, labelsLayout);

		Button cancelButton = new Button(msg.getMessage("Wizard.cancel"), e -> cancelTask.run());
		Button startOverButton = new Button(msg.getMessage("Wizard.start-over"), e -> init());

		nextButton.setText(msg.getMessage("Wizard.next"));
		nextButton.addClickListener(event -> nextStep());

		backButton.setText(msg.getMessage("Wizard.back"));
		backButton.addClickListener(event -> backStep());

		finishButton.setText(msg.getMessage("Wizard.finish"));
		finishButton.addClickListener(e ->
		{
			nextButton.setEnabled(false);
			finishButton.setEnabled(false);
			backButton.setEnabled(false);
			progressBar.setValue(1.0);
			WizardStep endStep = listIterator.previous().getEndStep();
			contentLayout.removeAll();
			contentLayout.add(endStep);
			endStep.run();
		});

		buttonsLayout.add(cancelButton, startOverButton, backButton, nextButton, finishButton);
		buttonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		buttonsLayout.setWidthFull();

		contentLayout.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
		contentLayout.getStyle().set("border-radius", "var(--lumo-border-radius-m)");

		add(titleComponent, labelsLayout, progressBar, contentLayout, buttonsLayout);
		getStyle().set("gap", "0");
		init();
	}

	private void initLabelsLayout(List<WizardStepController<?, ?>> steps, HorizontalLayout labelsLayout)
	{
		labelsLayout.setWidthFull();
		steps.forEach(wizardStepController ->
		{
			WizardStep currentStep = wizardStepController.getCurrentStep();
			if(currentStep.label == null)
				return;
			HorizontalLayout label = new HorizontalLayout(new Label(currentStep.label));
			label.setWidthFull();
			label.setJustifyContentMode(JustifyContentMode.CENTER);
			labelsLayout.add(label);
		});
	}

	private void backStep()
	{
		listIterator.previous();
		WizardStep currentStep = listIterator.previous().getCurrentStep();
		listIterator.next();
		contentLayout.removeAll();
		contentLayout.add(currentStep);
		currentStep.run();
		if(progressBar.getValue() == 1.0)
		{
			progressBar.setValue(1.0 - (progressBarIncrementer / 2));
			return;
		}
		double value = progressBar.getValue() - progressBarIncrementer;
		progressBar.setValue(Math.max(value, 0));

		backButton.setEnabled(listIterator.hasPrevious());
	}

	private void nextStep()
	{
		contentLayout.removeAll();
		WizardStep currentStep = listIterator.next().getCurrentStep();
		contentLayout.add(currentStep);
		currentStep.run();

		backButton.setEnabled(true);
		if(progressBar.getValue() == 0)
		{
			progressBar.setValue(progressBarIncrementer / 2);
			return;
		}
		double value = progressBar.getValue() + progressBarIncrementer;
		progressBar.setValue(Math.min(value, 1.0));

		nextButton.setEnabled(false);
	}

	private void init()
	{
		this.listIterator = new LinkedList<>(steps).listIterator();
		contentLayout.removeAll();
		progressBar.setValue(progressBarIncrementer / 2);
		contentLayout.add(listIterator.next().getCurrentStep());
		nextButton.setEnabled(true);
		backButton.setEnabled(false);
		finishButton.setEnabled(false);
	}

	void readyToGo()
	{
		ui.access(() ->
		{
			if(listIterator.hasNext())
				nextButton.setEnabled(true);
			else
				finishButton.setEnabled(true);
		});
	}

	void go()
	{
		ui.access(() ->
		{
			if(listIterator.hasNext())
				nextStep();
			else
				finishButton.setEnabled(true);
		});
	}

	public static WizardBuilder builder()
	{
		return new WizardBuilder();
	}

	public static final class WizardBuilder
	{
		private final List<WizardStepController<?, ?>> stepControllers = new ArrayList<>();
		private Runnable cancelTask;
		private MessageSource msg;
		private String title;

		private WizardBuilder()
		{
		}

		public WizardBuilder addStepControllers(WizardStepController<?, ?> stepController)
		{
			stepControllers.add(stepController);
			stepController.getCurrentStep().setWizardStepController(stepController);
			return this;
		}

		public WizardBuilder addMessageSource(MessageSource msg)
		{
			this.msg = msg;
			return this;
		}

		public WizardBuilder addCancelTask(Runnable cancelTask)
		{
			this.cancelTask = cancelTask;
			return this;
		}

		public WizardBuilder title(String title)
		{
			this.title = title;
			return this;
		}

		public Wizard build()
		{
			Wizard wizard = new Wizard(stepControllers, msg, cancelTask, title);
			stepControllers.forEach(stepController -> stepController.setWizard(wizard));
			return wizard;
		}
	}
}
