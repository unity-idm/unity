/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home.connectid;

import org.vaadin.teemu.wizards.Wizard;

import pl.edu.icm.unity.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.sandbox.wizard.AbstractSandboxWizardProvider;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

/**
 * Configures account association wizard.
 * @author K. Benedyczak
 */
public class ConnectIdWizardProvider extends AbstractSandboxWizardProvider
{
	private UnityMessageSource msg;

	public ConnectIdWizardProvider(UnityMessageSource msg, String sandboxURL, SandboxAuthnNotifier sandboxNotifier)
	{
		super(sandboxURL, sandboxNotifier);
		this.msg = msg;
		this.wizard = initWizard(); 
	}

	@Override
	protected Wizard initWizard()
	{
		final Wizard wizard = new Wizard();
		wizard.setSizeFull();
		wizard.addStep(new IntroStep(msg));
		ConfirmationStep confirmationStep = new ConfirmationStep(msg);
		wizard.addStep(confirmationStep);
		
		addSandboxListener();
		configureNextButtonWithPopupOpen(wizard, IntroStep.class);
		return wizard;
	}

	@Override
	protected void handle(SandboxAuthnEvent event)
	{
		// TODO Auto-generated method stub
		
	}
}
