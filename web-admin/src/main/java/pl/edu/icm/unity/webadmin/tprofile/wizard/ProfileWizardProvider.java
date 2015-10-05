/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile.wizard;

import org.vaadin.teemu.wizards.Wizard;

import com.vaadin.ui.UI;

import pl.edu.icm.unity.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.sandbox.wizard.AbstractSandboxWizardProvider;
import pl.edu.icm.unity.server.authn.remote.RemoteSandboxAuthnContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.tprofile.TranslationProfileEditDialog.Callback;
import pl.edu.icm.unity.webadmin.tprofile.TranslationProfileEditor;
import pl.edu.icm.unity.webui.association.IntroStep;
import pl.edu.icm.unity.webui.association.SandboxStep;

/**
 * Creates input profile creation wizard
 * @author K. Benedyczak
 */
public class ProfileWizardProvider extends AbstractSandboxWizardProvider
{
	private UnityMessageSource msg;
	private Callback addCallback;
	private TranslationProfileEditor editor;

	public ProfileWizardProvider(UnityMessageSource msg, String sandboxURL, SandboxAuthnNotifier sandboxNotifier, 
			TranslationProfileEditor editor, Callback addCallback)
	{
		super(sandboxURL, sandboxNotifier);
		this.msg = msg;
		this.editor = editor;
		this.addCallback = addCallback;
	}

	@Override
	public Wizard getWizardInstance()
	{
		final Wizard wizard = new Wizard();
		wizard.setSizeFull();
		
		final SandboxStep sandboxStep = new SandboxStep(msg, sandboxURL, wizard);
		final ProfileStep profileStep = new ProfileStep(msg, editor, addCallback);
		
		wizard.addStep(new IntroStep(msg, "Wizard.IntroStepComponent.introLabel"));
		wizard.addStep(sandboxStep);
		wizard.addStep(profileStep);
		
		//for the initial page
		openSandboxPopupOnNextButton(wizard);
		
		//and when the page is loaded with back button
		showSandboxPopupAfterGivenStep(wizard, IntroStep.class);

		addSandboxListener(new HandlerCallback()
		{
			@Override
			public void handle(SandboxAuthnEvent event)
			{
				RemoteSandboxAuthnContext sandboxedCtx = ((RemoteSandboxAuthnContext) event.getCtx()); 
				profileStep.handle(sandboxedCtx.getAuthnContext().getAuthnInput());
				sandboxStep.enableNext();
				wizard.next();
				wizard.getBackButton().setEnabled(false);
			}
		}, wizard, UI.getCurrent());
		
		return wizard;
	}

	@Override
	public String getCaption()
	{
		return msg.getMessage("Wizard.wizardCaption");
	}
}
