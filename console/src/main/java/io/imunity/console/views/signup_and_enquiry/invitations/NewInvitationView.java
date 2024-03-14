/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.invitations;

import static io.imunity.console.views.EditViewActionLayoutFactory.createActionLayout;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.CommonViewParam;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.console.views.signup_and_enquiry.invitations.editor.InvitationEditor;
import io.imunity.vaadin.elements.BreadCrumbParameter;
import io.imunity.vaadin.elements.NotificationPresenter;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import io.imunity.vaadin.endpoint.common.exceptions.ControllerException;

/**
 * New invitation view.
 * 
 * @author P.Piernik
 *
 */
@PermitAll
@Route(value = "/invitations/new", layout = ConsoleMenu.class)
public class NewInvitationView extends ConsoleViewComponent
{
	private final InvitationsService controller;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private BreadCrumbParameter breadCrumbParameter;
	private InvitationEditor editor;

	NewInvitationView(InvitationsService controller, MessageSource msg, NotificationPresenter notificationPresenter)
	{
		this.controller = controller;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String param)
	{

		Location location = event.getLocation();
		Map<String, List<String>> queryParameters = location.getQueryParameters()
				.getParameters();

		String type = Optional.ofNullable(queryParameters.getOrDefault(CommonViewParam.type.name(), null))
				.map(l -> l.get(0))
				.orElse(null);
		String name = Optional.ofNullable(queryParameters.getOrDefault(CommonViewParam.name.name(), null))
				.map(l -> l.get(0))
				.orElse(null);

		breadCrumbParameter = new BreadCrumbParameter(null, msg.getMessage("new"));

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
			notificationPresenter.showError(e.getCaption(), e.getMessage());

			UI.getCurrent()
					.navigate(InvitationsView.class);
			return;
		}

		getContent().removeAll();
		getContent().add(
				new VerticalLayout(editor, createActionLayout(msg, false, InvitationsView.class, this::onConfirm)));
	}

	@Override
	public Optional<BreadCrumbParameter> getDynamicParameter()
	{
		return Optional.ofNullable(breadCrumbParameter);
	}

	private void onConfirm()
	{

		InvitationParam invitation;
		try
		{
			invitation = editor.getInvitation();
		} catch (FormValidationException e)
		{		
			notificationPresenter.showError(msg.getMessage("Generic.formError"), msg.getMessage("Generic.formErrorHint"));
			return;
		}

		try
		{
			controller.addInvitation(invitation);
		} catch (ControllerException e)
		{

			notificationPresenter.showError(e.getCaption(), e.getMessage());
			return;
		}

		UI.getCurrent()
				.navigate(InvitationsView.class);

	}
}
