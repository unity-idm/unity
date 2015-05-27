/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile.wizard;

import org.vaadin.teemu.wizards.Wizard;

import pl.edu.icm.unity.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.sandbox.SandboxAuthnNotifier;
import pl.edu.icm.unity.sandbox.wizard.AbstractSandboxWizardProvider;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.tprofile.TranslationProfileEditDialog.Callback;
import pl.edu.icm.unity.webadmin.tprofile.TranslationProfileEditor;

/**
 * Creates input profile creation wizard
 * @author K. Benedyczak
 */
public class ProfileWizardProvider extends AbstractSandboxWizardProvider
{
	private SandboxStep sandboxStep;
	private ProfileStep profileStep;
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
		this.wizard = initWizard();
	}

	@Override
	protected Wizard initWizard()
	{
		final Wizard wizard = new Wizard();
		wizard.setImmediate(false);
		wizard.setWidth("100.0%");
		wizard.setHeight("100.0%");
		
		sandboxStep = new SandboxStep(msg, sandboxURL);
		profileStep = new ProfileStep(msg, editor, addCallback);
		
		wizard.addStep(new IntroStep(msg));
		wizard.addStep(sandboxStep);
		wizard.addStep(profileStep);
		
		//for the initial page
		openSandboxPopupOnNextButton(wizard);
		
		//and when the page is loaded with back button
		configureNextButtonWithPopupOpen(wizard, IntroStep.class);
		
		return wizard;
	}

	@Override
	protected void handle(SandboxAuthnEvent event)
	{
		profileStep.handle(event.getCtx().getAuthnContext().getAuthnInput());
		sandboxStep.enableNext();
		wizard.next();
		wizard.getBackButton().setEnabled(false);
	}
}
