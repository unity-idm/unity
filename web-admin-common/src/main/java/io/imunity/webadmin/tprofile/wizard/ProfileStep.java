/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webadmin.tprofile.wizard;

import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.ui.Component;

import io.imunity.webadmin.tprofile.TranslationProfileEditor;
import io.imunity.webadmin.tprofile.TranslationProfileEditDialog.Callback;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Third wizard step with with profile creation - used in {@link WizardDialogComponent}.
 * 
 * @author Roman Krysinski
 */
public class ProfileStep implements WizardStep 
{
	private UnityMessageSource msg;
	private ProfileStepComponent profileComponent;
	private TranslationProfileEditor editor;
	private Callback addCallback;

	public ProfileStep(UnityMessageSource msg, TranslationProfileEditor editor, 
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
