/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.enquiry;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.NotificationPresenter;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.EnquiryForm.EnquiryType;
import pl.edu.icm.unity.types.registration.EnquiryResponse;

/**
 * Dialog allowing to fill an enquiry form. It takes an editor component as argument.
 * Dialog uses 3 buttons: submit request, ignore (only if form is not mandatory) and cancel.
 * 
 * @author K. Benedyczak
 */
public class EnquiryFormFillDialog extends ConfirmDialog
{
	private final EnquiryResponseEditor editor;
	private final Callback callback;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;

	public EnquiryFormFillDialog(MessageSource msg, NotificationPresenter notificationPresenter, String header,
	                             EnquiryResponseEditor editor, Callback callback, EnquiryType type)
	{
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
		this.editor = editor;
		this.callback = callback;
		setSizeFull();
		setHeader(header);

		setConfirmText(msg.getMessage("RegistrationRequestEditorDialog.submitRequest"));
		addConfirmListener(e -> onConfirm());

		setCancelText(type == EnquiryType.REQUESTED_MANDATORY ? msg.getMessage("MainHeader.logout") : msg.getMessage("cancel"));
		addCancelListener(e -> onCancel());
		setCancelable(true);
		if (type == EnquiryType.REQUESTED_OPTIONAL)
		{
			setRejectText(msg.getMessage("EnquiryFormFillDialog.ignore"));
			setRejectable(true);
			addRejectListener(e -> callback.ignored());
		}
		add(getContents());
	}
	
	protected VerticalLayout getContents()
	{
		VerticalLayout layout = new VerticalLayout();
		layout.add(editor);
		layout.setAlignItems(FlexComponent.Alignment.CENTER);;
		layout.setMargin(false);
		layout.setPadding(false);
		return layout;
	}

	protected void onCancel()
	{
		callback.cancelled();
	}
	
	protected void onConfirm()
	{
		EnquiryResponse request = editor.getRequestWithStandardErrorHandling(true).orElse(null);
		if (request == null)
		{
			open();
			return;
		}
		try
		{
			callback.newRequest(request);
			close();
		} catch (Exception e)
		{
			handleFormSubmissionError(e, msg, editor);
			open();
		}
	}

	public void handleFormSubmissionError(Exception e, MessageSource msg, EnquiryResponseEditor editor)
	{
		if (e instanceof IllegalFormContentsException)
		{
			editor.markErrorsFromException((IllegalFormContentsException) e);
			if (e instanceof IllegalFormContentsException.OccupiedIdentityUsedInRequest)
			{
				String identity = ((IllegalFormContentsException.OccupiedIdentityUsedInRequest) e).occupiedIdentity.getValue();
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
	
	public interface Callback
	{
		void newRequest(EnquiryResponse request) throws WrongArgumentException;
		void cancelled();
		void ignored();
	}
}
