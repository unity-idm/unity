/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.input_profiles.wizard;

import java.util.function.Consumer;

import com.vaadin.flow.component.Component;

import io.imunity.console.tprofile.TranslationProfileEditor;
import io.imunity.vaadin.endpoint.common.wizard.WizardStep;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.webui.common.FormValidationException;

class AddProfileStep extends WizardStep
{
	private final TranslationProfileEditor editor;
	private final Consumer<TranslationProfile> addProfile;

	public AddProfileStep(String label, Component component, TranslationProfileEditor editor,
			Consumer<TranslationProfile> addProfile)
	{
		super(label, component);
		this.editor = editor;
		this.addProfile = addProfile;
	}

	@Override
	protected void initialize()
	{
		try
		{
			TranslationProfile profile = editor.getProfile();
			addProfile.accept(profile);
		} catch (FormValidationException e)
		{
			goToPrevStep();
			return;
		}

		stepComplited();
		refreshWizard();
	}
}
