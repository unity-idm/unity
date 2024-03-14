/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.input_profiles.wizard;

import com.vaadin.flow.component.Component;
import io.imunity.console.tprofile.TranslationProfileEditor;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.wizard.WizardStep;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

import java.util.function.Consumer;

class AddProfileStep extends WizardStep
{
	private final TranslationProfileEditor editor;
	private final Consumer<TranslationProfile> addProfile;
	private final NotificationPresenter notificationPresenter;
	private final MessageSource msg;
	
	public AddProfileStep(String label, Component component, TranslationProfileEditor editor,
			Consumer<TranslationProfile> addProfile, NotificationPresenter notificationPresenter, MessageSource msg)
	{
		super(label, component);
		this.editor = editor;
		this.addProfile = addProfile;
		this.notificationPresenter = notificationPresenter;
		this.msg = msg;
	}

	@Override
	protected void initialize()
	{
		try
		{
			TranslationProfile profile = editor.getProfile();
			addProfile.accept(profile);
			wizard.close();
			
		} catch (FormValidationException e)
		{
			notificationPresenter.showError(msg.getMessage("Generic.formError"), msg.getMessage("Generic.formErrorHint"));
			stepRequiredPrevStep();
			refreshWizard();
			return;
		} 

		stepComplited();
		refreshWizard();
	}
}
