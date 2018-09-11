/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.identities.ext;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.UserError;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.VerifiableEmail;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.attributes.ext.TextFieldWithVerifyButton;
import pl.edu.icm.unity.webui.common.identities.IdentityEditor;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorContext;
import pl.edu.icm.unity.webui.confirmations.ConfirmationInfoFormatter;

/**
 * {@link EmailIdentity} editor
 * @author P. Piernik
 */
public class EmailIdentityEditor implements IdentityEditor
{
	private Logger log = Log.getLogger(Log.U_SERVER_WEB, EmailIdentityEditor.class);
	private UnityMessageSource msg;
	private ConfirmationInfo confirmationInfo;
	private TextFieldWithVerifyButton editor;
	private boolean skipUpdate = false;
	private IdentityParam value;
	private EmailConfirmationManager emailConfirmationMan;
	private EntityResolver idResolver;
	private ConfirmationInfoFormatter formatter;
	private IdentityEditorContext context;
	
	public EmailIdentityEditor(UnityMessageSource msg, EmailConfirmationManager emailConfirmationMan, EntityResolver idResolver, ConfirmationInfoFormatter formatter)
	{
		this.msg = msg;
		this.emailConfirmationMan = emailConfirmationMan;
		this.idResolver = idResolver;
		this.formatter = formatter;	
	}

	@Override
	public ComponentsContainer getEditor(IdentityEditorContext context)
	{
		this.context = context;
		confirmationInfo = new ConfirmationInfo();	
		editor = new TextFieldWithVerifyButton(context.isAdminMode(), context.isRequired(), msg.getMessage(
				"EmailIdentityEditor.resendConfirmation"),
				Images.messageSend.getResource(),
				msg.getMessage("EmailIdentityEditor.confirmedCheckbox"));
		
		ComponentsContainer ret = new ComponentsContainer(editor);
		setLabel(new EmailIdentity().getHumanFriendlyName(msg));
		
		editor.addVerifyButtonClickListener(e -> {

			if (value != null)
			{
				ConfirmDialog confirm = new ConfirmDialog(msg, msg
						.getMessage("EmailIdentityEditor.confirmResendConfirmation"),
						() -> { sendConfirmation();
							confirmationInfo.setSentRequestAmount(confirmationInfo.getSentRequestAmount() + 1);
							updateConfirmationStatusIcon();
						      });
				confirm.show();
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
		
		updateConfirmationStatusIcon();
			
		return ret;

	}
	
	private void sendConfirmation()
	{
		try
		{
			emailConfirmationMan.sendVerificationNoTx(new EntityParam(value), idResolver.getFullIdentity(value), true);
			
		} catch (EngineException e1)
		{
			
			log.debug("Cannot send cofirmation request", e1);
			NotificationPopup.showError(msg, msg.getMessage(
					"EmailIdentityEditor.confirmationSendError"), e1);
		
		}
	}

	private void updateConfirmationStatusIcon()
	{
		if (value == null)
		{
			editor.setConfirmationStatusIconVisiable(false);;
		} else
		{
			editor.setConfirmationStatusIcon(
					formatter.getSimpleConfirmationStatusString(
							confirmationInfo),
					confirmationInfo.isConfirmed());
		}
		editor.setVerifyButtonVisiable(!confirmationInfo.isConfirmed()
				&& !editor.getValue().isEmpty() && value != null
				&& editor.getValue().equals(value.getValue()));
		skipUpdate = true;
		editor.setAdminCheckBoxValue(confirmationInfo.isConfirmed());
		skipUpdate = false;
	}

	@Override
	public IdentityParam getValue() throws IllegalIdentityValueException
	{
		String emailVal = editor.getValue().trim();
		if (emailVal.equals(""))
		{
			if (!context.isRequired())
				return null;
			String err = msg.getMessage("EmailIdentityEditor.emailEmpty");
			editor.setComponentError(new UserError(err));
			throw new IllegalIdentityValueException(err);
		}
		editor.setComponentError(null);
		
		try
		{
			new EmailIdentity().validate(emailVal);
		} catch (IllegalArgumentException e)
		{
			editor.setComponentError(new UserError(e.getMessage()));
			throw e;
		}
		
		VerifiableEmail ve = new VerifiableEmail(emailVal, confirmationInfo);
		return EmailIdentity.toIdentityParam(ve, null, null);
	}

	@Override
	public void setDefaultValue(IdentityParam value)
	{
		VerifiableEmail ve = EmailIdentity.fromIdentityParam(value);
		this.value = value;
		confirmationInfo = ve.getConfirmationInfo();
		editor.setValue(ve.getValue());
		updateConfirmationStatusIcon();
	}

	@Override
	public void setLabel(String value)
	{
		if (context.isShowLabelInline())
			editor.setPlaceholder(value);
		else
			editor.setCaption(value + ":");
	}
}
