/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.inputTranslation;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webadmin.tprofile.TranslationProfileEditor;
import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.authentication.inputTranslation.InputTranslationsView.InputTranslationsNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.helpers.StandardButtonsHelper;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.translation.ProfileMode;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Edit input translation view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class EditInputTranslationView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "EditInputTranslation";

	private UnityMessageSource msg;
	private InputTranslationsController controller;
	private TranslationProfileEditor editor;
	private String profileName;

	public EditInputTranslationView(UnityMessageSource msg, InputTranslationsController controller)
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
			NotificationPopup.showError(e);
			NavigationHelper.goToView(InputTranslationsView.VIEW_NAME);
			return;
		}

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(editor);

		HorizontalLayout buttons = null;
		if (!editor.isReadOnlyMode())
		{
			buttons = StandardButtonsHelper.buildConfirmEditButtonsBar(msg,
					() -> onConfirm(), () -> onCancel());
		} else
		{
			buttons = StandardButtonsHelper.buildShowButtonsBar(msg,
					() -> onCancel());
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

			NotificationPopup.showError(e);
			return;
		}

		NavigationHelper.goToView(InputTranslationsView.VIEW_NAME);

	}

	private void onCancel()
	{
		NavigationHelper.goToView(InputTranslationsView.VIEW_NAME);

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
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Override
	public String getDisplayedName()
	{
		return profileName;
	}

	@Component
	public static class EditInputTranslationNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public EditInputTranslationNavigationInfoProvider(InputTranslationsNavigationInfoProvider parent,
				ObjectFactory<EditInputTranslationView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedView)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory)
					.build());

		}
	}
}
