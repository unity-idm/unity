/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile.dryrun;

import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

import pl.edu.icm.unity.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.sandbox.wizard.AbstractSandboxWizardProvider;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.tprofile.wizard.IntroStep;

/**
 * Configures profile dry run wizard 
 * @author K. Benedyczak
 */
public class DryRunWizardProvider extends AbstractSandboxWizardProvider
{
	private DryRunStep dryrunStep;
	private UnityMessageSource msg;
	private TranslationActionsRegistry registry;
	private TranslationProfileManagement tpMan;

	public DryRunWizardProvider(UnityMessageSource msg, String sandboxURL, SandboxAuthnNotifier sandboxNotifier, 
			TranslationActionsRegistry registry, TranslationProfileManagement tpMan)
	{
		super(sandboxURL, sandboxNotifier);
		this.msg = msg;
		this.registry = registry;
		this.tpMan = tpMan;
		this.wizard = initWizard();
	}

	@Override
	protected Wizard initWizard()
	{
		final Wizard wizard = new Wizard();
		wizard.setSizeFull();
		dryrunStep = new DryRunStep(msg, sandboxURL, registry, tpMan);
		wizard.addStep(new IntroStep(msg));
		wizard.addStep(dryrunStep);
		
		//for the initial page
		openSandboxPopupOnNextButton(wizard);
		
		//and when the page is loaded with back button
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
				if (event.getActivatedStep() instanceof IntroStep) 
					openSandboxPopupOnNextButton(wizard);
			}
		});
		return wizard;
	}

	@Override
	protected void handle(SandboxAuthnEvent event)
	{
		dryrunStep.handle(event);
	}
}
