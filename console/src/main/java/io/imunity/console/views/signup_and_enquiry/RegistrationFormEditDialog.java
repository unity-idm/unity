/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import io.imunity.vaadin.elements.DialogWithActionFooter;
import io.imunity.vaadin.elements.NotificationPresenter;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

public class RegistrationFormEditDialog extends DialogWithActionFooter
{

	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private final RegistrationFormEditor editor;
	private final Callback callback;

	public RegistrationFormEditDialog(MessageSource msg, String caption, Callback callback,
			RegistrationFormEditor attributeEditor, NotificationPresenter notificationPresenter)
	{
		super(msg::getMessage);
		this.msg = msg;
		this.editor = attributeEditor;
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
			RegistrationForm form = editor.getForm();
			if (!preCheckForm(form))
				return;

			if (callback.newForm(form, editor.isIgnoreRequestsAndInvitations()))
				close();
		} catch (FormValidationException e)
		{
			notificationPresenter.showError(msg.getMessage("Generic.formError"), e.getMessage());
		}
	}

	private boolean preCheckForm(RegistrationForm form)
	{
		if (form.isPubliclyAvailable() && form.containsAutomaticAndMandatoryParams() && form.hasAnyLocalCredential())
		{
			new ConfirmDialog(
					msg.getMessage("ConfirmDialog.confirm"),
					msg.getMessage("RegistrationFormEditDialog.publiclAndRemoteWarning"),
					msg.getMessage("ok"),
					e -> {
						if (callback.newForm(form, editor.isIgnoreRequestsAndInvitations()))
							this.close();
					},
					msg.getMessage("cancel"),
					e -> {}
			).open();
			return false;
		}
		return true;
	}

	public interface Callback
	{
		boolean newForm(RegistrationForm form, boolean ignoreRequests);
	}
}
