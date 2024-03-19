/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry;

import io.imunity.vaadin.elements.DialogWithActionFooter;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.EnquiryForm;

public class EnquiryFormEditDialog extends DialogWithActionFooter
{
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private final EnquiryFormEditor editor;
	private final Callback callback;

	public EnquiryFormEditDialog(MessageSource msg, String caption, Callback callback,
			EnquiryFormEditor editor, NotificationPresenter notificationPresenter)
	{
		super(msg::getMessage);
		this.msg = msg;
		this.editor = editor;
		this.callback = callback;
		this.notificationPresenter = notificationPresenter;
		setHeaderTitle(caption);
		setActionButton(msg.getMessage("ok"), this::onConfirm);
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
