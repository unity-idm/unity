/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directorySetup.attributeClasses;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.directorySetup.attributeClasses.AttributeClassesView.AttributeClassesNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Edit attribute class view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class EditAttributeClassView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "EditAttributeClass";

	private AttributeClassController controller;
	private MessageSource msg;
	private AttributesClassEditor editor;

	private String attributeClassName;

	EditAttributeClassView(AttributeClassController controller, MessageSource msg)
	{
		this.controller = controller;
		this.msg = msg;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{

		attributeClassName = NavigationHelper.getParam(event, CommonViewParam.name.toString());

		AttributesClass ac;
		try
		{
			ac = controller.getAttributeClass(attributeClassName);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(AttributeClassesView.VIEW_NAME);
			return;
		}

		try
		{
			editor = controller.getEditor(ac);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(AttributeClassesView.VIEW_NAME);
			return;
		}
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(editor);
		main.addComponent(StandardButtonsHelper.buildConfirmEditButtonsBar(msg, () -> onConfirm(),
				() -> onCancel()));
		setCompositionRoot(main);
	}

	private void onConfirm()
	{
		AttributesClass attrClass;
		try
		{
			attrClass = editor.getAttributesClass();
		} catch (FormValidationException e)
		{
			return;
		}

		if (attrClass == null)
			return;

		try
		{
			controller.updateAttributeClass(attrClass);
		} catch (ControllerException e)
		{

			NotificationPopup.showError(msg, e);
			return;
		}

		NavigationHelper.goToView(AttributeClassesView.VIEW_NAME);

	}

	private void onCancel()
	{
		NavigationHelper.goToView(AttributeClassesView.VIEW_NAME);

	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Override
	public String getDisplayedName()
	{
		return attributeClassName;
	}

	@Component
	public static class EditAttributeClassNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public EditAttributeClassNavigationInfoProvider(ObjectFactory<EditAttributeClassView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedView)
					.withParent(AttributeClassesNavigationInfoProvider.ID).withObjectFactory(factory).build());

		}
	}

}
