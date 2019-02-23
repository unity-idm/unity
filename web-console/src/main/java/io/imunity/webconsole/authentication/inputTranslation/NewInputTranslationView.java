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
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Add input translation view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class NewInputTranslationView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "NewInputTranslation";

	private UnityMessageSource msg;
	private InputTranslationsController controller;
	private TranslationProfileEditor editor;

	public NewInputTranslationView(UnityMessageSource msg, InputTranslationsController controller)
	{
		this.msg = msg;
		this.controller = controller;
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
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
			NotificationPopup.showError(e);
			NavigationHelper.goToView(InputTranslationsView.VIEW_NAME);
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

			NotificationPopup.showError(e);
			return;
		}

		NavigationHelper.goToView(InputTranslationsView.VIEW_NAME);

	}

	private void onCancel()
	{
		NavigationHelper.goToView(InputTranslationsView.VIEW_NAME);

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

	@Component
	public static class NewInputTranslationNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public NewInputTranslationNavigationInfoProvider(InputTranslationsNavigationInfoProvider parent,
				ObjectFactory<NewInputTranslationView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedView)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory).build());

		}
	}
}
