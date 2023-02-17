/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.account_association.wizard;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import pl.edu.icm.unity.MessageSource;

import java.util.ArrayList;
import java.util.List;

public class Wizard extends VerticalLayout
{
	private final Button finishButton = new Button();
	private final Button nextButton = new Button();
	private final Button backButton = new Button();
	private final Button startOverButton = new Button();
	private final ProgressBar progressBar = new ProgressBar();
	private final double progressBarIncrementer;
	private final VerticalLayout contentLayout = new VerticalLayout();
	private final UI ui = UI.getCurrent();
	private final WizardStepController wizardStepController;


	public Wizard(List<WizardStep> steps, List<WizardStepPreparer<?,?>> stepPreparers, MessageSource msg, Runnable cancelTask, String title)
	{
		this.wizardStepController = new WizardStepController(steps, stepPreparers);
		List<String> labels = steps.stream()
				.map(step -> step.label).toList();
		this.progressBarIncrementer = 1.0/(steps.size() - 1);

		H2 titleComponent = new H2(title);
		titleComponent.getStyle().set("margin-top", "0");
		HorizontalLayout labelsLayout = new HorizontalLayout();
		HorizontalLayout buttonsLayout = new HorizontalLayout();

		initLabelsLayout(labels, labelsLayout);

		Button cancelButton = new Button(msg.getMessage("Wizard.cancel"), e -> cancelTask.run());
		startOverButton.setText(msg.getMessage("Wizard.start-over"));
		startOverButton.addClickListener(e -> init());

		nextButton.setText(msg.getMessage("Wizard.next"));
		nextButton.addClickListener(event -> nextStep());
		nextButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		backButton.setText(msg.getMessage("Wizard.back"));
		backButton.addClickListener(event -> prevStep());

		finishButton.setText(msg.getMessage("Wizard.finish"));
		finishButton.addClickListener(e ->
		{
			nextButton.setEnabled(false);
			finishButton.setEnabled(false);
			backButton.setEnabled(false);
			progressBar.setValue(1.0);
			contentLayout.removeAll();
			contentLayout.add(wizardStepController.getNext());
		});
		finishButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		buttonsLayout.add(cancelButton, startOverButton, backButton, nextButton, finishButton);
		buttonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		buttonsLayout.setWidthFull();

		contentLayout.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
		contentLayout.getStyle().set("border-radius", "var(--lumo-border-radius-m)");

		add(titleComponent, labelsLayout, progressBar, contentLayout, buttonsLayout);
		getStyle().set("gap", "0");
		init();
	}

	private void initLabelsLayout(List<String> labels, HorizontalLayout labelsLayout)
	{
		labelsLayout.setWidthFull();
		labels.forEach(label ->
		{
			if(label == null)
				return;
			HorizontalLayout labelComponent = new HorizontalLayout(new Label(label));
			labelComponent.setWidthFull();
			labelComponent.setJustifyContentMode(JustifyContentMode.CENTER);
			labelsLayout.add(labelComponent);
		});
	}

	private void init()
	{
		wizardStepController.startAgain();
		contentLayout.removeAll();
		progressBar.setValue(progressBarIncrementer / 2);
		contentLayout.add(wizardStepController.getCurrent());
		nextButton.setEnabled(true);
		backButton.setEnabled(false);
		finishButton.setEnabled(false);
		startOverButton.setEnabled(false);
	}

	private void nextStep()
	{
		if(wizardStepController.hasFinished())
		{
			finishButton.setEnabled(true);
			return;
		}
		contentLayout.removeAll();
		contentLayout.add(wizardStepController.getNext());

		if(progressBar.getValue() == 0)
			progressBar.setValue(progressBarIncrementer / 2);
		else
		{
			double value = progressBar.getValue() + progressBarIncrementer;
			progressBar.setValue(Math.min(value, 1.0));
		}
		nextButton.setEnabled(false);
		backButton.setEnabled(true);
		startOverButton.setEnabled(true);
	}
	private void prevStep()
	{
		contentLayout.removeAll();
		contentLayout.add(wizardStepController.getPrev());
		if(progressBar.getValue() == 1.0)
			progressBar.setValue(1.0 - (progressBarIncrementer / 2));
		else
		{
			double value = progressBar.getValue() - progressBarIncrementer;
			progressBar.setValue(Math.max(value, 0));
		}

		backButton.setEnabled(wizardStepController.hasPrev());
		startOverButton.setEnabled(wizardStepController.hasPrev());
		finishButton.setEnabled(false);
	}

	void refresh()
	{
		WizardStep current = wizardStepController.getCurrent();
		if(current.getStatus() == WizardStepStatus.COMPLITED)
			stepComplited();
		else if (current.getStatus() == WizardStepStatus.NEXT_STEP_REQUIRED)
			ui.access(this::nextStep);
	}

	private void stepComplited()
	{
		ui.access(() ->
		{
			if(wizardStepController.hasFinished())
				finishButton.setEnabled(true);
			else
				nextButton.setEnabled(true);
		});
	}

	public static WizardBuilder builder()
	{
		return new WizardBuilder();
	}

	public static final class WizardBuilder
	{
		private final List<WizardStep> wizardSteps = new ArrayList<>();
		private final List<WizardStepPreparer<?, ?>> stepPreparers = new ArrayList<>();
		private Runnable cancelTask;
		private MessageSource msg;
		private String title;

		private WizardBuilder()
		{
		}

		public WizardBuilder addStep(WizardStep wizardStep)
		{
			wizardSteps.add(wizardStep);
			return this;
		}

		public WizardBuilder addNextStepPreparer(WizardStepPreparer<?, ?> preparer)
		{
			stepPreparers.add(preparer);
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
			Wizard wizard = new Wizard(wizardSteps, stepPreparers, msg, cancelTask, title);
			wizardSteps.forEach(wizardStep -> wizardStep.setWizard(wizard));
			return wizard;
		}
	}
}
