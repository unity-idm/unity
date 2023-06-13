/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.translationProfile;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.tprofile.TranslationProfileEditor;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

public abstract class NewTranslationView extends CustomComponent implements UnityView
{
	private MessageSource msg;
	private TranslationsControllerBase controller;
	private TranslationProfileEditor editor;
	
	@Autowired
	protected NewTranslationView(MessageSource msg, TranslationsControllerBase controller)
	{
		this.msg = msg;
		this.controller = controller;
	}	

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("new");
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		String toClone = NavigationHelper.getParam(event, CommonViewParam.name.toString());
		try
		{
			editor = getEditor(toClone);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(getViewAllName());
			return;
		}

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(editor);
		main.addComponent(StandardButtonsHelper.buildConfirmNewButtonsBar(msg,
				() -> onConfirm(), () -> onCancel()));
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
			controller.addProfile(profile);

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
	
	private TranslationProfileEditor getEditor(String toClone) throws ControllerException
	{
		TranslationProfileEditor editor = controller.getEditor();
		if (toClone != null && !toClone.isEmpty())
		{

			TranslationProfile profile = controller.getProfile(toClone);
			editor.setValue(profile);
			editor.setCopyMode();
		}
		return editor;
	}
	
	public abstract String getViewName();
	public abstract String getViewAllName();
}
