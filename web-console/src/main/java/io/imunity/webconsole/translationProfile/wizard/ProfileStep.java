/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.translationProfile.wizard;

import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.ui.Component;

import io.imunity.webconsole.tprofile.TranslationProfileEditor;
import io.imunity.webconsole.tprofile.TranslationProfileEditDialog.Callback;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;

/**
 * Third wizard step with with profile creation - used in {@link WizardDialogComponent}.
 * 
 * @author Roman Krysinski
 */
class ProfileStep implements WizardStep 
{
	private MessageSource msg;
	private ProfileStepComponent profileComponent;
	private TranslationProfileEditor editor;
	private Callback addCallback;

	ProfileStep(MessageSource msg, TranslationProfileEditor editor, 
			Callback addCallback) 
	{
		this.msg = msg;
		this.editor = editor;
		this.addCallback = addCallback;
		profileComponent = new ProfileStepComponent(msg, editor);
	}

	void handle(RemotelyAuthenticatedInput authnInput) 
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
