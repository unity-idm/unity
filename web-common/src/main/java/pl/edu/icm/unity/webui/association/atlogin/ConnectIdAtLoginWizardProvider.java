/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.association.atlogin;

import org.vaadin.teemu.wizards.Wizard;

import com.vaadin.ui.UI;

import pl.edu.icm.unity.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.sandbox.wizard.AbstractSandboxWizardProvider;
import pl.edu.icm.unity.server.authn.remote.InputTranslationEngine;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.association.IntroStep;
import pl.edu.icm.unity.webui.association.SandboxStep;

/**
 * Configures account association wizard. The sandbox login shall return existing user, to associate 
 * the given unknown user with.
 * 
 * @author K. Benedyczak
 */
public class ConnectIdAtLoginWizardProvider extends AbstractSandboxWizardProvider
{
	private UnityMessageSource msg;
	private InputTranslationEngine translationEngine;
	private RemotelyAuthenticatedContext unknownUser;

	public ConnectIdAtLoginWizardProvider(UnityMessageSource msg, String sandboxURL, SandboxAuthnNotifier sandboxNotifier,
			InputTranslationEngine translationEngine, RemotelyAuthenticatedContext unknownUser)
	{
		super(sandboxURL, sandboxNotifier);
		this.msg = msg;
		this.translationEngine = translationEngine;
		this.unknownUser = unknownUser;
	}

	@Override
	public Wizard getWizardInstance()
	{
		final Wizard wizard = new Wizard();
		wizard.setSizeFull();
		wizard.addStep(new IntroStep(msg, "ConnectIdAtLoginWizardProvider.introLabel"));
		final SandboxStep sandboxStep = new SandboxStep(msg, sandboxURL, wizard);
		wizard.addStep(sandboxStep);
		final MergeUnknownWithExistingConfirmationStep confirmationStep = 
				new MergeUnknownWithExistingConfirmationStep(msg, translationEngine, wizard, 
						unknownUser);
		wizard.addStep(confirmationStep);
		
		openSandboxPopupOnNextButton(wizard);
		showSandboxPopupAfterGivenStep(wizard, IntroStep.class);
		addSandboxListener(new HandlerCallback()
		{
			@Override
			public void handle(SandboxAuthnEvent event)
			{
				sandboxStep.enableNext();
				confirmationStep.setAuthnData(event);
				wizard.next();						
			}
		}, wizard, UI.getCurrent());
		return wizard;
	}

	@Override
	public String getCaption()
	{
		return msg.getMessage("ConnectId.wizardCaption");
	}
}
