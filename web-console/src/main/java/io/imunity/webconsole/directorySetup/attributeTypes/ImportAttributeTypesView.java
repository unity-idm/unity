/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directorySetup.attributeTypes;

import java.util.Set;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.directorySetup.attributeTypes.AttributeTypesView.AttributeTypesNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
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
 * Import attribute type view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class ImportAttributeTypesView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "ImportAttributeTypes";

	private AttributeTypeController controller;
	private MessageSource msg;
	private ImportAttributeTypeEditor editor;

	@Autowired
	ImportAttributeTypesView(AttributeTypeController controller, MessageSource msg)
	{
		super();
		this.controller = controller;
		this.msg = msg;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		try
		{
			editor = controller.getImportEditor();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(AttributeTypesView.VIEW_NAME);
			return;
		}

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(editor);
		main.addComponent(StandardButtonsHelper.buildConfirmButtonsBar(msg, msg.getMessage("ImportAttributeTypesView.import"), () -> onConfirm(),
				() -> onCancel()));
		setCompositionRoot(main);
	}

	private void onConfirm()
	{
		Set<AttributeType> ats;
		try
		{
			ats = editor.getAttributeTypes();
		} catch (Exception e)
		{
			return;
		}

		try
		{
			controller.mergeAttributeTypes(ats, editor.isOverwriteMode());
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
		return msg.getMessage("ImportAttributeTypesView.caption");
	}

	@Component
	public static class ImportAttributeTypeNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		@Autowired
		public ImportAttributeTypeNavigationInfoProvider(ObjectFactory<ImportAttributeTypesView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedView)
					.withParent(AttributeTypesNavigationInfoProvider.ID).withObjectFactory(factory).build());

		}
	}

}
