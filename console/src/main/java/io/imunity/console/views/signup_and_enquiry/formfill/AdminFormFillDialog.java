/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry.formfill;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.forms.BaseRequestEditor;
import pl.edu.icm.unity.base.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.BaseRegistrationInput;

/**
 * Dialog allowing to fill an enquiry or registration form. Intended to be used
 * from the AdminUI. It takes an editor component as argument. Dialog uses 3
 * buttons: submit request, submit and accept, cancel.
 * 
 * @author K. Benedyczak
 */
class AdminFormFillDialog<T extends BaseRegistrationInput> extends Dialog
{
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private final BaseRequestEditor<T> editor;
	private final Callback<T> callback;
	
	AdminFormFillDialog(MessageSource msg, NotificationPresenter notificationPresenter, String caption,
			BaseRequestEditor<T> editor, Callback<T> callback)
	{
		this.msg = msg;
		this.editor = editor;
		this.callback = callback;
		this.notificationPresenter = notificationPresenter;

		initUI(caption);
	}

	private void initUI(String caption)
	{
		setHeaderTitle(caption);
		Button submit = new Button(msg.getMessage("UserFormFillDialog.submitRequest"));
		submit.addClickListener(e -> onConfirm(false));
		submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		
		Button cancel = new Button(msg.getMessage("cancel"));
		cancel.addClickListener(e -> onCancel());

		Button submitAndAccept = new Button(msg.getMessage("UserFormFillDialog.submitAndAccept"));
		submitAndAccept.addClickListener(e -> onConfirm(true));

		getFooter().add(submitAndAccept, cancel, submit);
		add(getContents());
		setWidth(90, Unit.EM);
		setHeight(70, Unit.EM);
		setCloseOnOutsideClick(false);
	}

	private VerticalLayout getContents()
	{
		VerticalLayout vl = new VerticalLayout();
		vl.setMargin(false);
		vl.setSpacing(false);
		vl.add(editor);
		vl.setSizeFull();
		return vl;
	}

	private void onCancel()
	{
		callback.cancelled();
		close();
	}

	private void onConfirm(boolean autoAccept)
	{
		try
		{
			T request = editor.getRequestWithStandardErrorHandling(true)
					.orElse(null);
			if (request == null)
				return;
			callback.newRequest(request, autoAccept);
			close();
		} catch (Exception e)
		{
			handleFormSubmissionError(e);
			return;
		}
	}

	public void handleFormSubmissionError(Exception e)
	{
		if (e instanceof IllegalFormContentsException)
		{
			editor.markErrorsFromException((IllegalFormContentsException) e);
			if (e instanceof IllegalFormContentsException.OccupiedIdentityUsedInRequest)
			{
				String identity = ((IllegalFormContentsException.OccupiedIdentityUsedInRequest) e).occupiedIdentity
						.getValue();
				notificationPresenter.showError(msg.getMessage("FormRequest.occupiedIdentity", identity), "");
			} else
			{
				notificationPresenter.showError(msg.getMessage("Generic.formError"), e.getMessage());
			}
		} else
		{
			notificationPresenter.showError(msg.getMessage("Generic.formError"), e.getMessage());
		}
	}

	interface Callback<T>
	{
		void newRequest(T request, boolean autoAccept) throws WrongArgumentException;
		void cancelled();
	}
}
