/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directorySetup.attributeTypes;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.directorySetup.attributeTypes.AttributeTypesView.AttributeTypesNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Edit attribute type view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class EditAttributeTypeView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "EditAttributeType";

	private AttributeTypeController controller;
	private MessageSource msg;
	private AttributeTypeEditor editor;

	private String attributeTypeName;

	@Autowired
	EditAttributeTypeView(AttributeTypeController controller, MessageSource msg)
	{
		super();
		this.controller = controller;
		this.msg = msg;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		attributeTypeName = NavigationHelper.getParam(event, CommonViewParam.name.toString());

		AttributeType at;
		try
		{
			at = controller.getAttributeType(attributeTypeName);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(AttributeTypesView.VIEW_NAME);
			return;
		}

		editor = controller.getEditor(at);

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(editor.getComponent());
		main.addComponent(StandardButtonsHelper.buildConfirmEditButtonsBar(msg, () -> onConfirm(),
				() -> onCancel()));
		setCompositionRoot(main);
	}

	private void onConfirm()
	{
		AttributeType at;
		try
		{
			at = editor.getAttributeType();
		} catch (Exception e)
		{
			return;
		}

		try
		{
			controller.updateAttributeType(at);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			return;
		}

		NavigationHelper.goToView(AttributeTypesView.VIEW_NAME);

	}

	private void onCancel()
	{
		NavigationHelper.goToView(AttributeTypesView.VIEW_NAME);

	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Override
	public String getDisplayedName()
	{
		return attributeTypeName;
	}

	@Component
	public static class EditAttributeTypeNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		@Autowired
		public EditAttributeTypeNavigationInfoProvider(ObjectFactory<EditAttributeTypeView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedView)
					.withParent(AttributeTypesNavigationInfoProvider.ID).withObjectFactory(factory).build());

		}
	}

}
