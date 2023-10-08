/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_setup.identity_types;

import static io.imunity.console.views.EditViewActionLayoutFactory.createActionLayout;

import java.util.Optional;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.BreadCrumbParameter;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.WebSession;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Edit identityType view
 * 
 * @author P.Piernik
 *
 */
@PermitAll
@Route(value = "/identity-type/edit", layout = ConsoleMenu.class)
class EditIdentityTypeView extends ConsoleViewComponent
{
	public static final String VIEW_NAME = "EditIdentityType";

	private final IdentityTypesController controller;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;

	private IdentityTypeEditor editor;
	private BreadCrumbParameter breadCrumbParameter;

	EditIdentityTypeView(IdentityTypesController controller, MessageSource msg, NotificationPresenter notificationPresenter)
	{
		this.controller = controller;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
		
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String identityTypeId)
	{
		getContent().removeAll();

		IdentityType idType;
		try
		{
			idType = controller.getIdentityType(identityTypeId);
			breadCrumbParameter = new BreadCrumbParameter(identityTypeId, identityTypeId);
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getCause().getMessage());
			return;
		}

		initUI(idType);
	}

	@Override
	public Optional<BreadCrumbParameter> getDynamicParameter()
	{
		return Optional.ofNullable(breadCrumbParameter);
	}

	public void initUI(IdentityType type)
	{
		this.editor = controller.getEditor(type);
		getContent().add(
				new VerticalLayout(editor, createActionLayout(msg, true, IdentityTypesView.class, this::onConfirm)));
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
			controller.updateIdentityType(idType, WebSession.getCurrent()
					.getEventBus());
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getCause().getMessage());
			return;
		}
		UI.getCurrent()
				.navigate(IdentityTypesView.class);
	}

}
