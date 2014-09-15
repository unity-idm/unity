/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile.wizard;

import java.util.concurrent.atomic.AtomicBoolean;

import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

import pl.edu.icm.unity.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.tprofile.TranslationProfileEditDialog.Callback;
import pl.edu.icm.unity.webadmin.tprofile.TranslationProfileEditor;

import com.vaadin.event.UIEvents.PollEvent;
import com.vaadin.event.UIEvents.PollListener;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Dialog window that deals with profile wizard.
 * 
 * @author Roman Krysinski
 */
public class WizardDialog extends Window implements WizardProgressListener
{
	private WizardDialogComponent wizardComponent;
	private String sandboxURL;
	private AtomicBoolean isAuthnEventArrived;
	private SandboxAuthnNotifier sandboxNotifier;
	private SandboxAuthnNotifier.Listener sandboxListener;

	public WizardDialog(UnityMessageSource msg, String sandboxURL, SandboxAuthnNotifier sandboxNotifier, 
			TranslationProfileEditor editor, Callback addCallback, Callback updateCallback)
	{
		this.sandboxURL      = sandboxURL;
		this.sandboxNotifier = sandboxNotifier;
		isAuthnEventArrived  = new AtomicBoolean(false);
		wizardComponent      = new WizardDialogComponent(msg, sandboxURL, editor, addCallback, updateCallback);
		wizardComponent.addWizardListener(this);
		
		openSandboxPopupOnNextButton();
		
		addSandboxListener();
		
		addPoolListener();
		
		setModal(true);
		setClosable(false);
		setWidth(80, Unit.PERCENTAGE);
		setHeight(85, Unit.PERCENTAGE);
		setContent(wizardComponent);
	}

	private void openSandboxPopupOnNextButton() 
	{
		SandboxPopup popup = new SandboxPopup(new ExternalResource(sandboxURL));
		popup.attachButtonOnce(wizardComponent.getNextButton());	
	}

	
	private void addSandboxListener() 
	{
		sandboxListener = new SandboxAuthnNotifier.Listener() 
		{
			@Override
			public void handle(SandboxAuthnEvent event) 
			{
				try 
				{
					VaadinSession.getCurrent().lock();
					wizardComponent.handle(event);
					isAuthnEventArrived.set(true);
				} finally 
				{
					VaadinSession.getCurrent().unlock();
				}				
			}
		};
		
		sandboxNotifier.addListener(sandboxListener);
	}
	
	/**
	 * Once sanbox popup window is opened, the {@value SandboxPopup.POLLING_INTERVAL} ms
	 * interval polling is enabled. This is to let the admin gui quickly detect when 
	 * sandbox event arrives and then disable polling.
	 */
	private void addPoolListener() 
	{
		UI.getCurrent().addPollListener(new PollListener() 
		{
			@Override
			public void poll(PollEvent event) 
			{
				if (isAuthnEventArrived.get()) 
				{
					disablePolling();
					isAuthnEventArrived.set(false);
				}
			}
		});
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
		sandboxNotifier.removeListener(sandboxListener);
		disablePolling();
	}

	@Override
	public void activeStepChanged(WizardStepActivationEvent event) 
	{
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
	
	private void disablePolling() 
	{
		if (UI.getCurrent().getPollInterval() != -1) 
		{
			UI.getCurrent().setPollInterval(-1);
		}
	}	
}
