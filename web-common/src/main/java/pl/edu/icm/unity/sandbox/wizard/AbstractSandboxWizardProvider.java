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

import pl.edu.icm.unity.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.sandbox.SandboxAuthnNotifier.AuthnResultListener;

import com.vaadin.server.ExternalResource;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.UI;

/**
 * Base class for sandbox wizard providers. The class provides the common code used to activate sandbox popup
 * at a desired wizard step. Also communication with the sandboxed UI is handled here. 
 *  
 * @author K. Benedyczak
 */
public abstract class AbstractSandboxWizardProvider
{
	protected SandboxAuthnNotifier sandboxNotifier;
	protected String callerId;
	protected String sandboxURL;

	
	public AbstractSandboxWizardProvider(String sandboxURL, SandboxAuthnNotifier sandboxNotifier)
	{
		this.sandboxURL = sandboxURL;
		this.sandboxNotifier = sandboxNotifier;
		this.callerId = VaadinService.getCurrentRequest().getWrappedSession().getId();
	}
	
	public abstract String getCaption();
	public abstract Wizard getWizardInstance();
	
	protected void openSandboxPopupOnNextButton(Wizard wizard) 
	{
		SandboxPopup popup = new SandboxPopup(new ExternalResource(sandboxURL));
		popup.attachButtonOnce(wizard.getNextButton());	
	}

	
	protected void addSandboxListener(final HandlerCallback callback, Wizard wizard) 
	{
		AuthnResultListener listener = new SandboxAuthnNotifier.AuthnResultListener() 
		{
			@Override
			public void handle(final SandboxAuthnEvent event) 
			{
				
				if (!callerId.equals(event.getCallerId()))
				{
					return;
				}
				
				UI.getCurrent().access(new Runnable()
				{
					@Override
					public void run()
					{
						callback.handle(event);
						UI.getCurrent().setPollInterval(-1);
					}
				});
			}
		};
		sandboxNotifier.addListener(listener);
		removeSandboxListenerOnCompleting(wizard, listener);
	}

	protected void removeSandboxListenerOnCompleting(final Wizard wizard, final AuthnResultListener listener)
	{
		wizard.addListener(new WizardProgressListener()
		{
			@Override
			public void wizardCompleted(WizardCompletedEvent event)	
			{
				sandboxNotifier.removeListener(listener);
			}
			
			@Override
			public void wizardCancelled(WizardCancelledEvent event)	
			{
				sandboxNotifier.removeListener(listener);
			}
			
			@Override
			public void stepSetChanged(WizardStepSetChangedEvent event) {}
			
			@Override
			public void activeStepChanged(WizardStepActivationEvent event) {}
		});
	
	}
	
	protected void showSandboxPopupAfterGivenStep(final Wizard wizard, final Class<?> prePopupStepClass)
	{
		wizard.addListener(new WizardProgressListener()
		{
			@Override
			public void wizardCompleted(WizardCompletedEvent event)	{}
			@Override
			public void wizardCancelled(WizardCancelledEvent event)	{}
			@Override
			public void stepSetChanged(WizardStepSetChangedEvent event) {}
			
			@Override
			public void activeStepChanged(WizardStepActivationEvent event)
			{
				if (event.getActivatedStep().getClass().isAssignableFrom(prePopupStepClass)) 
					openSandboxPopupOnNextButton(wizard);
			}
		});
	
	}
	
	protected interface HandlerCallback
	{
		void handle(SandboxAuthnEvent event);
	}
}
