/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sandbox.wizard;

import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

import com.vaadin.server.ExternalResource;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.UI;

import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnNotifier.AuthnResultListener;

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

	
	protected void addSandboxListener(SandboxAuthnNotifier.AuthnResultListener callback, Wizard wizard, 
			boolean stopOnFinal) 
	{
		AuthnResultListener listener = new SandboxAuthnNotifier.AuthnResultListener() 
		{
			private final UI parentUI = UI.getCurrent();
			private final InvocationContext invocationContext = InvocationContext.getCurrent();
			
			@Override
			public void onSandboxAuthnResult(final SandboxAuthnEvent event) 
			{
				if (!callerId.equals(event.callerId))
					return;
				parentUI.access(() -> invokeInOriginalContext(() -> 
				{
					callback.onSandboxAuthnResult(event);
					if (!stopOnFinal)
						parentUI.setPollInterval(-1);
				}));
			}
			
			private void invokeInOriginalContext(Runnable code)
			{
				if (InvocationContext.hasCurrent())
					throw new IllegalStateException("Invocation context set in thread");
				InvocationContext.setCurrent(invocationContext);
				try
				{
					code.run();
				} finally
				{
					InvocationContext.setCurrent(null);
				}
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
}
