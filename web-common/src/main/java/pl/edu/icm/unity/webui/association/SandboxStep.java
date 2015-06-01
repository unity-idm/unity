/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
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

import pl.edu.icm.unity.sandbox.wizard.SandboxPopup;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.server.ExternalResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Second wizard step with sandbox authentication to get input 
 * for profile creation- used in {@link WizardDialogComponent}.
 * 
 * @author Roman Krysinski
 */
public class SandboxStep extends CustomComponent implements WizardStep 
{

	private UnityMessageSource msg;
	private boolean onAdvance;
	private Wizard wizard;

	public SandboxStep(UnityMessageSource msg, String sandboxURL, final Wizard wizard) 
	{
		this.msg = msg;
		this.wizard = wizard;
		onAdvance = false;
		buildMainLayout(msg, sandboxURL);
		wizard.addListener(new WizardProgressListener()
		{
			@Override
			public void wizardCompleted(WizardCompletedEvent event)
			{
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
				if (event.getActivatedStep() instanceof SandboxStep)
					wizard.getNextButton().setEnabled(false);
			}
		});
	}

	@Override
	public String getCaption() 
	{
		return msg.getMessage("Wizard.SandboxStep.caption");
	}

	@Override
	public Component getContent() 
	{
		return this;
	}

	private void buildMainLayout(UnityMessageSource msg, String sandboxURL) 
	{
		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setWidth("100%");
		mainLayout.setMargin(true);
		mainLayout.setSpacing(true);
		setWidth("100.0%");
		setHeight("-1px");
		Label infoLabel = new Label();
		infoLabel.setWidth("100.0%");
		mainLayout.addComponent(infoLabel);
		Button sboxButton = new Button();
		mainLayout.addComponent(sboxButton);
		sboxButton.setCaption(msg.getMessage("Wizard.SandboxStepComponent.sboxButton"));
		SandboxPopup popup = new SandboxPopup(new ExternalResource(sandboxURL));
		popup.attachButton(sboxButton);
		
		infoLabel.setValue(msg.getMessage("Wizard.SandboxStepComponent.infoLabel"));

		setCompositionRoot(mainLayout);
	}

	@Override
	public boolean onAdvance() 
	{
		return onAdvance;
	}

	@Override
	public boolean onBack() 
	{
		return true;
	}
	
	public void enableNext()
	{
		onAdvance = true;
		wizard.getNextButton().setEnabled(true);
	}

}
