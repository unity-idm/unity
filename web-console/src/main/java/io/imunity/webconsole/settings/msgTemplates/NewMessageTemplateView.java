/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.settings.msgTemplates;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.settings.msgTemplates.MessageTemplatesView.MessageTemplatesNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Add new message template view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class NewMessageTemplateView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "NewMessageTemplate";

	private MessageTemplateController controller;
	private MessageSource msg;
	private MessageTemplateEditor editor;

	NewMessageTemplateView(MessageTemplateController controller, MessageSource msg)
	{
		this.controller = controller;
		this.msg = msg;
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

		editor = controller.getEditor(null);
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(editor);
		main.addComponent(StandardButtonsHelper.buildConfirmNewButtonsBar(msg, () -> onConfirm(),
				() -> onCancel()));
		setCompositionRoot(main);
	}

	private void onConfirm()
	{
		
		MessageTemplate template = editor.getTemplate();
		
		if (template == null)
			return;

		try
		{
			controller.addMessageTemplate(template);
		} catch (ControllerException e)
		{

			NotificationPopup.showError(msg, e);
			return;
		}

		NavigationHelper.goToView(MessageTemplatesView.VIEW_NAME);

	}

	private void onCancel()
	{
		NavigationHelper.goToView(MessageTemplatesView.VIEW_NAME);

	}

	@Component
	public static class NewMessageTemplateNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public NewMessageTemplateNavigationInfoProvider(ObjectFactory<NewMessageTemplateView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedView)
					.withParent(MessageTemplatesNavigationInfoProvider.ID).withObjectFactory(factory).build());

		}
	}

}
