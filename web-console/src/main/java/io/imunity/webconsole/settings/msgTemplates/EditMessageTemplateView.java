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
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
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
 * Edit message template view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class EditMessageTemplateView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "EditMessageTemplate";

	private MessageTemplateController controller;
	private MessageSource msg;
	private String templateName;
	private MessageTemplateEditor editor;

	EditMessageTemplateView(MessageTemplateController controller, MessageSource msg)
	{
		super();
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
		return templateName;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		templateName = NavigationHelper.getParam(event, CommonViewParam.name.toString());

		try
		{
			editor = getEditor(templateName);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(MessageTemplatesView.VIEW_NAME);
			return;
		}

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(editor);
		main.addComponent(StandardButtonsHelper.buildConfirmEditButtonsBar(msg, () -> onConfirm(),
				() -> onCancel()));
		setCompositionRoot(main);
	}

	private MessageTemplateEditor getEditor(String template) throws ControllerException
	{
		MessageTemplate msgTemplate = controller.getMessageTemplate(template);
		return controller.getEditor(msgTemplate);
	}

	private void onConfirm()
	{
		MessageTemplate template = editor.getTemplate();

		if (template == null)
			return;

		try
		{
			controller.updateMessageTemplate(template);
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
	public static class EditMessageTemplateNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public EditMessageTemplateNavigationInfoProvider(ObjectFactory<EditMessageTemplateView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedView)
					.withParent(MessageTemplatesNavigationInfoProvider.ID).withObjectFactory(factory).build());

		}
	}

}
