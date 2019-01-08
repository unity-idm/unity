/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.association.atlogin;

import org.vaadin.teemu.wizards.Wizard;

import com.vaadin.ui.UI;

import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationEngine;
import pl.edu.icm.unity.webui.association.IntroStep;
import pl.edu.icm.unity.webui.association.SandboxStep;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.webui.sandbox.wizard.AbstractSandboxWizardProvider;

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
				new MergeUnknownWithExistingConfirmationStep(msg, unknownUser, translationEngine, wizard);
		wizard.addStep(confirmationStep);
		
		openSandboxPopupOnNextButton(wizard);
		showSandboxPopupAfterGivenStep(wizard, IntroStep.class);
		addSandboxListener(new SandboxAuthnNotifier.AuthnResultListener()
		{
			@Override
			public void onPartialAuthnResult(SandboxAuthnEvent event)
			{
			}

			@Override
			public void onCompleteAuthnResult(AuthenticatedEntity authenticatedEntity)
			{
				sandboxStep.enableNext();
				confirmationStep.setAuthenticatedUser(authenticatedEntity);
				wizard.next();						
			}
		}, wizard, UI.getCurrent(), true);
		return wizard;
	}

	@Override
	public String getCaption()
	{
		return msg.getMessage("ConnectId.wizardCaption");
	}
}
