/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile.wizard;

import org.apache.log4j.Logger;
import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.server.ExternalResource;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Dialog window that deals with profile wizard.
 * 
 * @author Roman Krysinski
 */
public class WizardDialog extends Window implements WizardProgressListener
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, WizardDialog.class);
	private WizardDialogComponent wizardComponent;
	private String sandboxURL;

	public WizardDialog(UnityMessageSource msg, String sandboxURL)
	{
		this.sandboxURL = sandboxURL;
		setModal(true);
		setClosable(false);
		setWidth(80, Unit.PERCENTAGE);
		setHeight(85, Unit.PERCENTAGE);
		wizardComponent = new WizardDialogComponent(msg, sandboxURL);
		wizardComponent.addWizardListener(this);
		openSandboxPopupOnNextButton();
		setContent(wizardComponent);
	}
	
	public void show()
	{
		UI.getCurrent().addWindow(this);
		focus();
	}
	
	public void close()
	{
		if (getParent() != null)
		{
			((UI) getParent()).removeWindow(this);
		}
		disablePolling();
	}

	@Override
	public void activeStepChanged(WizardStepActivationEvent event) 
	{
		LOG.debug("activeStepChanged");
		if (event.getActivatedStep() instanceof IntroStep) 
		{
			openSandboxPopupOnNextButton();
		}
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
	
	private void openSandboxPopupOnNextButton() 
	{
		SandboxPopup popup = new SandboxPopup(new ExternalResource(sandboxURL));
		popup.attachButtonOnce(wizardComponent.getNextButton());	
	}

	private void disablePolling() 
	{
		if (UI.getCurrent().getPollInterval() != -1) 
		{
			UI.getCurrent().setPollInterval(-1);
		}
	}	
}
