/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.association;

import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.WizardStep;
import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;

/**
 * Shows confirmation of the account association and invokes the operation. 
 * This class is base class. Extensions can work in one of two modes: either to require
 * to get an unknown user from sandbox login or the get an existing user from sandbox login.
 * Then the merge is performed in a proper way, depending on context.
 * @author K. Benedyczak
 */
public abstract class AbstractConfirmationStep extends CustomComponent implements WizardStep
{
	protected UnityMessageSource msg;
	protected HtmlLabel introLabel;
	protected InputTranslationEngine translationEngine;
	protected ErrorComponent errorComponent;
	
	public AbstractConfirmationStep(UnityMessageSource msg, InputTranslationEngine translationEngine, 
			Wizard wizard)
	{
		this.msg = msg;
		this.translationEngine = translationEngine;
		setCompositionRoot(buildMainLayout());
		wizard.addListener(new WizardProgressListener()
		{
			@Override
			public void wizardCompleted(WizardCompletedEvent event)
			{
				merge();
			}
			
			@Override
			public void wizardCancelled(WizardCancelledEvent event)
			{
			}
			
			@Override
			public void stepSetChanged(WizardStepSetChangedEvent event)
			{
			}
			
			@Override
			public void activeStepChanged(WizardStepActivationEvent event)
			{
				if (event.getActivatedStep() instanceof AbstractConfirmationStep)
				{
					wizard.getBackButton().setEnabled(false);
					if (errorComponent.isVisible())
						wizard.getFinishButton().setEnabled(false);
				}
			}
		});
	}

	protected abstract void merge();
	
	private VerticalLayout buildMainLayout() 
	{
		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setSizeFull();
		mainLayout.setSpacing(false);
		setSizeFull();
		
		introLabel = new HtmlLabel(msg);
		introLabel.setWidth(100, Unit.PERCENTAGE);
		mainLayout.addComponent(introLabel);
		
		errorComponent = new ErrorComponent();
		mainLayout.addComponent(errorComponent);
		errorComponent.setVisible(false);
		
		return mainLayout;
	}

	protected void setError(String message)
	{
		introLabel.setVisible(false);
		errorComponent.setVisible(true);
		errorComponent.setError(message);
	}
	
	@Override
	public String getCaption()
	{
		return msg.getMessage("ConnectId.ConfirmStep.caption");
	}

	@Override
	public Component getContent()
	{
		return this;
	}

	@Override
	public boolean onBack()
	{
		return false;
	}
}
