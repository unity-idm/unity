/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.invitations;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.signupAndEnquiry.invitations.InvitationsView.InvitationsNavigationInfoProvider;
import io.imunity.webconsole.signupAndEnquiry.invitations.editor.InvitationEditorV8;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * New invitation view.
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class NewInvitationView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "NewInvitation";

	private final InvitationsController controller;
	private final MessageSource msg;
	private InvitationEditorV8 editor;

	NewInvitationView(InvitationsController controller, MessageSource msg)
	{
		this.controller = controller;
		this.msg = msg;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		String type = NavigationHelper.getParam(event, CommonViewParam.type.toString());
		String name = NavigationHelper.getParam(event, CommonViewParam.name.toString());

		try
		{
			if (type != null && !type.isEmpty() && name != null && !name.isEmpty())
			{
				editor = controller.getEditor(type, name);
			} else
			{
				editor = controller.getEditor();
			}

		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(InvitationsView.VIEW_NAME);
			return;
		}

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(editor);
		main.addComponent(StandardButtonsHelper.buildConfirmNewButtonsBar(msg, () -> onConfirm(),
				() -> onCancel()));
		setCompositionRoot(main);
	}

	private void onConfirm()
	{

		InvitationParam invitation;
		try
		{
			invitation = editor.getInvitation();
		} catch (FormValidationException e)
		{
			return;
		}

		try
		{
			controller.addInvitation(invitation);
		} catch (ControllerException e)
		{

			NotificationPopup.showError(msg, e);
			return;
		}

		NavigationHelper.goToView(InvitationsView.VIEW_NAME);

	}

	private void onCancel()
	{
		NavigationHelper.goToView(InvitationsView.VIEW_NAME);

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

	@Component
	public static class NewInvitationNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public NewInvitationNavigationInfoProvider(ObjectFactory<NewInvitationView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedView)
					.withParent(InvitationsNavigationInfoProvider.ID).withObjectFactory(factory).build());

		}
	}
}
