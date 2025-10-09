/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.forms;

import static io.imunity.console.views.EditViewActionLayoutFactory.createActionLayout;

import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.console.views.signup_and_enquiry.EnquiryFormEditor;
import io.imunity.vaadin.elements.BreadCrumbParameter;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.exceptions.ControllerException;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.engine.api.utils.NameToURLEncoder;

@PermitAll
@Route(value = "/forms/enquiry", layout = ConsoleMenu.class)
public class EnquiryView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final EnquiryFormsController controller;
	private final NotificationPresenter notificationPresenter;
	private EnquiryFormEditor editor;
	private BreadCrumbParameter breadCrumbParameter;
	private boolean edit;

	EnquiryView(MessageSource msg, EnquiryFormsController controller,
			NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.controller = controller;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String registrationName)
	{
		String decodedregistrationName = registrationName == null ? null :  NameToURLEncoder.decode(registrationName);
		getContent().removeAll();
		try
		{
			if (decodedregistrationName == null)
			{
				breadCrumbParameter = new BreadCrumbParameter(null, msg.getMessage("new"));
				edit = false;
				Optional<String> clone = event.getLocation()
						.getQueryParameters()
						.getParameters()
						.getOrDefault("clone", List.of())
						.stream().findFirst();
				if (clone.isPresent())
					editor = controller.getEditor(controller.getEnquiryForm(NameToURLEncoder.decode(clone.get())), true);
				else
					editor = controller.getEditor(null, false);
			}
			else
			{
				editor = controller.getEditor(controller.getEnquiryForm(decodedregistrationName), false);
				breadCrumbParameter = new BreadCrumbParameter(decodedregistrationName, decodedregistrationName);
				edit = true;
			}

			getContent().add(
					new VerticalLayout(editor, createActionLayout(msg, edit, FormsView.class, this::onConfirm)));
		}
		catch (ControllerException exception)
		{
			notificationPresenter.showError(msg.getMessage("error"), exception.getMessage());
		}
	}

	@Override
	public Optional<BreadCrumbParameter> getDynamicParameter()
	{
		return Optional.ofNullable(breadCrumbParameter);
	}

	private void onConfirm()
	{

		EnquiryForm form;
		try
		{
			form = editor.getForm();
		}
		catch (FormValidationException e)
		{
			notificationPresenter.showError(msg.getMessage("Generic.formError"),
					e.getMessage() == null ? msg.getMessage("Generic.formErrorHint") : e.getMessage());
			return;
		}

		try
		{
			if(edit)
				controller.updateEnquiryForm(form, editor.isIgnoreRequestsAndInvitations());
			else
				controller.addEnquiryForm(form);
		}
		catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getCause().getMessage());
			return;
		}
		UI.getCurrent().navigate(FormsView.class);
	}

}
