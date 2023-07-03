/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.identities.email;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValueContext;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.StringBindingValue;
import io.imunity.vaadin.elements.TextFieldWithVerifyButton;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.SingleStringFieldBinder;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditor;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditorContext;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.base.verifiable.VerifiableEmail;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.webui.confirmations.ConfirmationInfoFormatter;

public class EmailIdentityEditor implements IdentityEditor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EmailIdentityEditor.class);
	private final MessageSource msg;
	private ConfirmationInfo confirmationInfo;
	private TextFieldWithVerifyButton editor;
	private boolean skipUpdate = false;
	private IdentityParam value;
	private final EmailConfirmationManager emailConfirmationMan;
	private final EntityResolver idResolver;
	private final ConfirmationInfoFormatter formatter;
	private SingleStringFieldBinder binder;
	private final NotificationPresenter notificationPresenter;

	public EmailIdentityEditor(MessageSource msg, EmailConfirmationManager emailConfirmationMan,
	                           EntityResolver idResolver, ConfirmationInfoFormatter formatter, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.emailConfirmationMan = emailConfirmationMan;
		this.idResolver = idResolver;
		this.formatter = formatter;	
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public ComponentsContainer getEditor(IdentityEditorContext context)
	{
		binder = new SingleStringFieldBinder(msg);
		confirmationInfo = new ConfirmationInfo();	
		editor = new TextFieldWithVerifyButton(context.isAdminMode(), 
				msg.getMessage("EmailIdentityEditor.resendConfirmation"),
				VaadinIcon.ENVELOPE_O.create(),
				msg.getMessage("EmailIdentityEditor.confirmedCheckbox"),
				context.isShowLabelInline());
		
		ComponentsContainer ret = new ComponentsContainer(editor);
		editor.setLabel(new EmailIdentity().getHumanFriendlyName(msg));
		
		editor.addVerifyButtonClickListener(e -> {

			if (value != null)
			{
				ConfirmDialog confirm = new ConfirmDialog(
						msg.getMessage("ConfirmDialog.confirm"),
						msg.getMessage("EmailIdentityEditor.confirmResendConfirmation"),
						"OK",
						event -> {
							sendConfirmation();
							confirmationInfo.setSentRequestAmount(confirmationInfo.getSentRequestAmount() + 1);
							updateConfirmationStatusIcon();
						});
				confirm.open();
			}

		});

		editor.addTextFieldValueChangeListener(e -> 
		{
			if (value != null)
			{
				if (e.getValue().equals(value.getValue()))
				{
					confirmationInfo = value.getConfirmationInfo();
				} else
				{
					confirmationInfo = new ConfirmationInfo();
				}
				updateConfirmationStatusIcon();
			}
		});

		editor.addAdminConfirmCheckBoxValueChangeListener(e -> 
		{
			if (!skipUpdate)
			{
				confirmationInfo = new ConfirmationInfo(e.getValue());
				updateConfirmationStatusIcon();
			}
		});
		
		if (!context.getConfirmationEditMode().isShowVerifyButton())
			editor.removeVerifyButton();
		if (!context.getConfirmationEditMode().isShowConfirmationStatus())
			editor.removeConfirmationStatusIcon();
		
		if (context.isCustomWidth())
			editor.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
		
		updateConfirmationStatusIcon();

		if(context.isRequired())
		{
			editor.setRequiredIndicatorVisible(true);
			editor.getElement().setProperty("title", msg.getMessage("fieldRequired"));
		}

		binder.forField(editor, context.isRequired())
			.withValidator((value1, context1) -> validate(value1, context1, context.isRequired()))
			.bind("value");
		binder.setBean(new StringBindingValue(""));
		
		return ret;

	}
	
	private void sendConfirmation()
	{
		try
		{
			emailConfirmationMan.sendVerificationNoTx(new EntityParam(value), idResolver.getFullIdentity(value), true);
			
		} catch (EngineException e1)
		{
			
			log.warn("Cannot send cofirmation request", e1);
			notificationPresenter.showError(msg.getMessage("EmailIdentityEditor.confirmationSendError"), e1.getMessage());
		}
	}

	private void updateConfirmationStatusIcon()
	{
		if (value == null)
		{
			editor.setConfirmationStatusIconVisiable(false);
		} else
		{
			editor.setConfirmationStatusIcon(
					formatter.getSimpleConfirmationStatusString(
							confirmationInfo),
					confirmationInfo.isConfirmed());
		}
		editor.setVerifyButtonVisible(!confirmationInfo.isConfirmed()
				&& !editor.getValue().isEmpty() && value != null
				&& editor.getValue().equals(value.getValue()));
		skipUpdate = true;
		editor.setAdminCheckBoxValue(confirmationInfo.isConfirmed());
		skipUpdate = false;
	}
	
	private ValidationResult validate(String value, ValueContext context, boolean required)
	{
		if (value.isEmpty() && !required)
			return ValidationResult.ok();
		else if (value.isEmpty())
			return ValidationResult.error(msg.getMessage("fieldRequired"));
		try
		{
			new EmailIdentity().validate(value);
			return ValidationResult.ok();
		} catch (IllegalIdentityValueException e)
		{
			return ValidationResult.error(e.getMessage());
		}
	}


	@Override
	public IdentityParam getValue() throws IllegalIdentityValueException
	{
		binder.ensureValidityCatched(() -> new IllegalIdentityValueException(""));
		String emailVal = binder.getBean().getValue().trim();
		if (emailVal.isEmpty())
			return null;
		
		VerifiableEmail ve = new VerifiableEmail(binder.getBean().getValue().trim(), confirmationInfo);
		return EmailIdentity.toIdentityParam(ve, null, null);
	}

	@Override
	public void setDefaultValue(IdentityParam value)
	{
		VerifiableEmail ve = EmailIdentity.fromIdentityParam(value);
		this.value = value;
		confirmationInfo = ve.getConfirmationInfo();
		binder.setBean(new StringBindingValue(ve.getValue()));
		updateConfirmationStatusIcon();
	}

	@Override
	public void setLabel(String value)
	{
		editor.setLabel(value);
	}
	
	
}
