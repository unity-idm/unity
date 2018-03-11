/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.UserError;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.TooManyAttempts;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * Base for credential reset step in which reset code must be sent - via email or sms.  
 * @author P.Piernik
 *
 */
public abstract class DynamicCredentialResetDialog extends AbstractDialog
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, DynamicCredentialResetDialog.class);
	protected UnityMessageSource msg;
	protected CredentialReset backend;
	protected String username;
	protected CredentialEditor credEditor;
	
	private TextField answer;
	private int expectedState;
	private String messageTemplate;
	private String answerLabel;
	private String resendDesc;
	private boolean onlyNumberCode;
	
	public DynamicCredentialResetDialog(UnityMessageSource msg, CredentialReset backend, CredentialEditor credEditor,
			String username,int expectedState, String messageTemplate, String answerLabel, String resendDesc, boolean onlyNumberCode)
	{
		super(msg, msg.getMessage("CredentialReset.title"), msg.getMessage("continue"),
				msg.getMessage("cancel"));
		this.msg = msg;
		this.backend = backend;
		this.username = username;
		this.credEditor = credEditor;
		this.expectedState = expectedState;
		this.messageTemplate = messageTemplate;
		this.answerLabel = answerLabel;
		this.resendDesc = resendDesc;
		this.onlyNumberCode = onlyNumberCode;
		
	}

	@Override
	protected Component getContents() throws Exception
	{
		if (CredentialResetStateVariable.get() != expectedState)
		{
			NotificationPopup.showError(msg, msg.getMessage("error"),
					msg.getMessage("CredentialReset.illegalAppState"));
			throw new Exception();
		}
		
		try
		{
			backend.sendCode(messageTemplate, onlyNumberCode);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("error"),
					msg.getMessage("CredentialReset.resetNotPossible"));
			CredentialResetStateVariable.reset();
			log.debug("Credential reset notification failed", e);
			throw e;
		}
		
		Label userLabel = new Label(msg.getMessage("CredentialReset.changingFor", username));
		
		answer = new TextField(answerLabel);
		final Button resend = new Button(msg.getMessage("CredentialReset.resend"));
		resend.setDescription(resendDesc);
		resend.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				try
				{
					backend.sendCode(messageTemplate, onlyNumberCode);
				} catch (TooManyAttempts e)
				{
					resend.setEnabled(false);
				} catch (Exception e)
				{
					log.debug("Credential reset notification failed", e);
					NotificationPopup.showError(msg, msg.getMessage("error"),
							msg.getMessage("CredentialReset.resetNotPossible"));
					onCancel();
				}
			}
		});
		VerticalLayout ret = new VerticalLayout();
		ret.setMargin(false);
		ret.addComponent(userLabel);
		FormLayout form = new FormLayout(answer, resend);
		ret.addComponent(form);
		return ret;
	}

	@Override
	protected void onCancel()
	{
		CredentialResetStateVariable.reset();
		super.onCancel();
	}
	
	@Override
	protected void onConfirm()
	{
		if (CredentialResetStateVariable.get() != expectedState)
			throw new IllegalStateException("Wrong application security state in password reset!" +
					" This should never happen.");

		String a = answer.getValue();
		if (a == null || a.equals(""))
		{
			answer.setComponentError(new UserError(msg.getMessage("fieldRequired")));
			return;
		}
		answer.setComponentError(null);

		try
		{
			backend.verifyDynamicData(a);
		} catch (TooManyAttempts e)
		{
			NotificationPopup.showError(msg, msg.getMessage("error"), 
					msg.getMessage("CredentialReset.codeInvalidOrExpired"));
			onCancel();
			return;
		} catch (Exception e)
		{
			answer.setValue("");
			NotificationPopup.showError(msg, msg.getMessage("error"), 
					msg.getMessage("CredentialReset.codeInvalid"));
			return;
		}
		
		
		
		close();

		CredentialResetStateVariable.inc(); //ok - next step allowed
		nextStep();
	}
	
	protected abstract void nextStep();
	
	
}
