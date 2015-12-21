/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile.wizard;

import org.vaadin.teemu.wizards.WizardStep;

import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.translation.TranslationProfile;
import pl.edu.icm.unity.server.translation.in.InputTranslationRule;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.tprofile.TranslationProfileEditDialog.Callback;
import pl.edu.icm.unity.webadmin.tprofile.TranslationProfileEditor;

import com.vaadin.ui.Component;

/**
 * Third wizard step with with profile creation - used in {@link WizardDialogComponent}.
 * 
 * @author Roman Krysinski
 */
public class ProfileStep implements WizardStep 
{
	private UnityMessageSource msg;
	private ProfileStepComponent profileComponent;
	private TranslationProfileEditor<InputTranslationRule> editor;
	private Callback addCallback;

	public ProfileStep(UnityMessageSource msg, TranslationProfileEditor<InputTranslationRule> editor, 
			Callback addCallback) 
	{
		this.msg = msg;
		this.editor = editor;
		this.addCallback = addCallback;
		profileComponent = new ProfileStepComponent(msg, editor);
	}

	public void handle(RemotelyAuthenticatedInput authnInput) 
	{
		profileComponent.handle(authnInput);
	}

	@Override
	public String getCaption() 
	{
		return msg.getMessage("Wizard.ProfileStep.caption");
	}

	@Override
	public Component getContent() 
	{
		return profileComponent;
	}

	@Override
	public boolean onAdvance() 
	{
		TranslationProfile translationProfile;
		try
		{
			translationProfile = editor.getProfile();
		} catch (Exception e)
		{
			return false;
		}
		return addCallback.handleProfile(translationProfile);
	}

	@Override
	public boolean onBack() 
	{
		return false;
	}
}
