/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.translationProfile;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.tprofile.TranslationProfileEditor;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileMode;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

public abstract class EditTranslationView extends CustomComponent implements UnityView
{
	private MessageSource msg;
	private TranslationsControllerBase controller;
	private TranslationProfileEditor editor;
	private String profileName;
	
	@Autowired
	protected EditTranslationView(MessageSource msg, TranslationsControllerBase controller)
	{
		this.msg = msg;
		this.controller = controller;	
	}
	
	@Override
	public void enter(ViewChangeEvent event)
	{
		profileName = NavigationHelper.getParam(event, CommonViewParam.name.toString());

		try
		{
			editor = getEditor(profileName);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(getViewAllName());
			return;
		}

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(editor);

		HorizontalLayout buttons = null;
		if (!editor.isReadOnlyMode())
		{
			buttons = StandardButtonsHelper.buildConfirmEditButtonsBar(msg, () -> onConfirm(),
					() -> onCancel());
		} else
		{
			buttons = StandardButtonsHelper.buildShowButtonsBar(msg, () -> onCancel());
		}

		main.addComponent(buttons);
		setCompositionRoot(main);
	}

	private void onConfirm()
	{

		TranslationProfile profile;
		try
		{
			profile = editor.getProfile();
		} catch (FormValidationException e)
		{
			return;
		}

		try
		{
			controller.updateProfile(profile);

		} catch (ControllerException e)
		{

			NotificationPopup.showError(msg, e);
			return;
		}

		NavigationHelper.goToView(getViewAllName());

	}

	private void onCancel()
	{
		NavigationHelper.goToView(getViewAllName());

	}

	private TranslationProfileEditor getEditor(String toEdit) throws ControllerException
	{
		TranslationProfile profile = controller.getProfile(profileName);
		editor = controller.getEditor();
		editor.setValue(profile);
		if (profile.getProfileMode().equals(ProfileMode.READ_ONLY))
		{
			editor.setReadOnlyMode();
		}
		return editor;

	}

	@Override
	public String getDisplayedName()
	{
		return profileName;
	}
	
	public abstract String getViewName();
	public abstract String getViewAllName();
}
