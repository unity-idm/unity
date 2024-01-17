/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import io.imunity.vaadin.elements.NotificationPresenter;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.webui.common.FormValidationException;

public class EnquiryFormEditDialog extends ConfirmDialog
{
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private final EnquiryFormEditor editor;
	private final Callback callback;

	public EnquiryFormEditDialog(MessageSource msg, String caption, Callback callback,
			EnquiryFormEditor editor, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.editor = editor;
		this.callback = callback;
		this.notificationPresenter = notificationPresenter;
		setHeader(caption);
		setConfirmButton(msg.getMessage("ok"), e -> onConfirm());
		setCancelButton(msg.getMessage("cancel"), e -> {});
		add(editor);
		setWidth("80%");
		setHeight("80%");
	}

	private void onConfirm()
	{
		try
		{
			EnquiryForm form = editor.getForm();
			if (callback.newForm(form, editor.isIgnoreRequestsAndInvitations()))
				close();
		}
		catch (FormValidationException e)
		{
			notificationPresenter.showError(msg.getMessage("Generic.formError"), e.getMessage());
		}
	}

	public interface Callback
	{
		boolean newForm(EnquiryForm form, boolean ignoreRequest);
	}
}
