/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directorySetup.identityTypes;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.directorySetup.identityTypes.IdentityTypesView.IdentityTypesNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.base.entity.IdentityType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Edit identityType view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class EditIdentityTypeView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "EditIdentityType";

	private IdentityTypesController controller;
	private MessageSource msg;
	private IdentityTypeEditor editor;
	private EventsBus bus;
	
	private String idTypeName;

	EditIdentityTypeView(IdentityTypesController controller, MessageSource msg)
	{
		this.controller = controller;
		this.msg = msg;
		this.bus = WebSession.getCurrent().getEventBus();
	}

	@Override
	public void enter(ViewChangeEvent event)
	{

		idTypeName = NavigationHelper.getParam(event, CommonViewParam.name.toString());

		IdentityType idType;
		try
		{
			idType = controller.getIdentityType(idTypeName);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(IdentityTypesView.VIEW_NAME);
			return;
		}

		editor = controller.getEditor(idType);

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(editor);
		main.addComponent(StandardButtonsHelper.buildConfirmEditButtonsBar(msg, () -> onConfirm(),
				() -> onCancel()));
		setCompositionRoot(main);
	}

	private void onConfirm()
	{
		IdentityType idType;
		try
		{
			idType = editor.getIdentityType();
		} catch (FormValidationException e)
		{
			return;
		}

		if (idType == null)
			return;

		try
		{
			controller.updateIdentityType(idType, bus);
		} catch (ControllerException e)
		{

			NotificationPopup.showError(msg, e);
			return;
		}

		NavigationHelper.goToView(IdentityTypesView.VIEW_NAME);

	}

	private void onCancel()
	{
		NavigationHelper.goToView(IdentityTypesView.VIEW_NAME);

	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Override
	public String getDisplayedName()
	{
		return idTypeName;
	}

	@Component
	public static class EditIdentityTypeNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public EditIdentityTypeNavigationInfoProvider(ObjectFactory<EditIdentityTypeView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedView)
					.withParent(IdentityTypesNavigationInfoProvider.ID).withObjectFactory(factory).build());

		}
	}

}
