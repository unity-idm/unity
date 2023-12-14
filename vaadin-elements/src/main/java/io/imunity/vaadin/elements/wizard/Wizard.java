/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements.wizard;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;

public class Wizard extends Dialog
{
	private final Button finishButton = new Button();
	private final Button nextButton = new Button();
	private final Button backButton = new Button();
	private final ProgressBar progressBar = new ProgressBar();
	private final double progressBarIncrementer;
	private final VerticalLayout contentLayout = new VerticalLayout();
	private final UI ui = UI.getCurrent();
	private final WizardStepController wizardStepController;


	public Wizard(List<WizardStep> steps, List<WizardStepPreparer<?, ?>> stepPreparers, Function<String, String> msg,
			Runnable cancelTask, String title)
	{
		setModal(true);
		setWidth("80%");
		setHeight("60%");
		
		contentLayout.setSizeUndefined();
//		contentLayout.setMargin(false);
//		contentLayout.setSpacing(false);
//		contentLayout.setPadding(false);
		
		
		this.wizardStepController = new WizardStepController(steps, stepPreparers);
		List<String> labels = steps.stream()
				.map(step -> step.label).toList();
		this.progressBarIncrementer = 1.0/(steps.size() - 1);

		HorizontalLayout labelsLayout = new HorizontalLayout();

		initLabelsLayout(labels, labelsLayout);

		Button cancelButton = new Button(msg.apply("Wizard.cancel"), e -> {cancelTask.run(); close();});
		cancelButton.setId("Wizard.cancel");

		nextButton.setText(msg.apply("Wizard.next"));
		nextButton.setId("Wizard.next");
		nextButton.addClickListener(event -> nextStep());
		nextButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		backButton.setText(msg.apply("Wizard.back"));
		backButton.setId("Wizard.back");
		backButton.addClickListener(event -> prevStep());

		finishButton.setText(msg.apply("Wizard.finish"));
		finishButton.setId("Wizard.finish");
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
		
		getFooter().add(cancelButton, backButton, nextButton, finishButton);
		getHeader().add(labelsLayout, progressBar);
		contentLayout.getStyle().set("background-color", "var(--unity-contrast)");
		contentLayout.getStyle().set("border-radius", "var(--unity-border-radius)");
		add(contentLayout);
	
		if (title != null)
			setHeaderTitle(title);
		init();
	}

	private void initLabelsLayout(List<String> labels, HorizontalLayout labelsLayout)
	{
		labelsLayout.setWidthFull();
		labels.forEach(label ->
		{
			if(label == null)
				return;
			HorizontalLayout labelComponent = new HorizontalLayout(new Span(label));
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
		nextButton.setVisible(true);
		backButton.setEnabled(false);
		finishButton.setEnabled(false);
		finishButton.setVisible(false);
	}

	private void showDisableFinishButton()
	{
		finishButton.setVisible(true);
		finishButton.setEnabled(false);
		nextButton.setVisible(false);
	}

	private void nextStep()
	{
		if(wizardStepController.hasFinished())
		{
			finishButton.setEnabled(true);
			finishButton.setVisible(true);
			nextButton.setVisible(false);
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
	}
	void prevStep()
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
		finishButton.setVisible(false);
		nextButton.setVisible(true);
	}

	void refresh()
	{
		WizardStep current = wizardStepController.getCurrent();
		if(current.getStatus() == WizardStepStatus.COMPLITED)
			stepComplited();
		else if (current.getStatus() == WizardStepStatus.NEXT_STEP_REQUIRED)
			ui.access(this::nextStep);
		else if (current.getStatus() == WizardStepStatus.PREV_STEP_REQUIRED)
			ui.access(this::prevStep);
	}

	void interrupt()
	{
		ui.access(this::showDisableFinishButton);
	}

	private void stepComplited()
	{
		ui.access(() ->
		{
			if(wizardStepController.hasFinished())
			{
				finishButton.setVisible(true);
				finishButton.setEnabled(true);
				nextButton.setVisible(false);
			}
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
		private Runnable cancelTask = () -> {};
		private Function<String, String> msg;
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

		public WizardBuilder addMessageSource(Function<String, String>  msg)
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
