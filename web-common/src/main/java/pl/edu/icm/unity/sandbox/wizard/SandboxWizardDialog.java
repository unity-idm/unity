/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.sandbox.wizard;

import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * Dialog window allowing for bootstrapping a wizard which opens a sandbox popup. Responsible for showing and closing 
 * the main dialog window with the {@link Wizard} component inside. 
 *  
 * @author K. Benedyczak
 */
public class SandboxWizardDialog extends Window implements WizardProgressListener
{
	public SandboxWizardDialog(Wizard wizard)
	{
		wizard.addListener(this);
		setModal(true);
		setClosable(false);
		setWidth(80, Unit.PERCENTAGE);
		setHeight(85, Unit.PERCENTAGE);
		setContent(getMainComponent(wizard));
	}

	private Component getMainComponent(Wizard wizard)
	{
		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setImmediate(false);
		mainLayout.setWidth("100%");
		mainLayout.setHeight("100%");
		mainLayout.setMargin(true);
		
		setWidth("100.0%");
		setHeight("100.0%");
		
		mainLayout.addComponent(wizard);
		return mainLayout;
	}
	
	public void show()
	{
		UI.getCurrent().addWindow(this);
		focus();
	}
	
	public void close()
	{
		if (getParent() != null)
			((UI) getParent()).removeWindow(this);
	}

	@Override
	public void activeStepChanged(WizardStepActivationEvent event) 
	{
		//nop
	}

	@Override
	public void stepSetChanged(WizardStepSetChangedEvent event) 
	{
		// nop
	}

	@Override
	public void wizardCompleted(WizardCompletedEvent event) 
	{
		close();
	}

	@Override
	public void wizardCancelled(WizardCancelledEvent event) 
	{
		close();
	}
}
