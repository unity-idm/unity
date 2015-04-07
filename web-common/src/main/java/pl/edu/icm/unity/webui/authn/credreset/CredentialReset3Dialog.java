/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset;

import org.apache.log4j.Logger;

import com.vaadin.server.UserError;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;

import pl.edu.icm.unity.exceptions.TooManyAttempts;
import pl.edu.icm.unity.server.authn.CredentialReset;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * 3rd step of credential reset pipeline. In this dialog the user must provide the reset code which was
 * sent via e-mail. In future other channels may be implemented here as SMS. 
 * <p>
 * This dialog checks at startup if the username exists, channel exists and the username has address - 
 * if not then error is shown and the pipline is closed. If everything is correct the code is sent. 
 * 
 * @author K. Benedyczak
 */
public class CredentialReset3Dialog extends AbstractDialog
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, CredentialReset3Dialog.class);
	private UnityMessageSource msg;
	private CredentialReset backend;
	private String username;
	private CredentialEditor credEditor;
	
	private TextField answer;
	
	public CredentialReset3Dialog(UnityMessageSource msg, CredentialReset backend, CredentialEditor credEditor,
			String username)
	{
		super(msg, msg.getMessage("CredentialReset.title"), msg.getMessage("continue"),
				msg.getMessage("cancel"));
		this.msg = msg;
		this.backend = backend;
		this.username = username;
		this.credEditor = credEditor;
	}

	@Override
	protected Component getContents() throws Exception
	{
		if (CredentialResetStateVariable.get() != 2)
		{
			NotificationPopup.showError(msg, msg.getMessage("error"),
					msg.getMessage("CredentialReset.illegalAppState"));
			throw new Exception();
		}
		
		try
		{
			backend.sendCode();
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("error"),
					msg.getMessage("CredentialReset.resetNotPossible"));
			CredentialResetStateVariable.reset();
			log.debug("Credential reset e-mail notification failed", e);
			throw e;
		}
		
		Label userLabel = new Label(msg.getMessage("CredentialReset.changingFor", username));
		
		answer = new TextField(msg.getMessage("CredentialReset.emailCode"));
		final Button resend = new Button(msg.getMessage("CredentialReset.resendEmail"));
		resend.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				try
				{
					backend.sendCode();
				} catch (TooManyAttempts e)
				{
					resend.setEnabled(false);
				} catch (Exception e)
				{
					log.debug("Credential reset e-mail notification failed", e);
					NotificationPopup.showError(msg, msg.getMessage("error"),
							msg.getMessage("CredentialReset.resetNotPossible"));
					onCancel();
				}
			}
		});
		VerticalLayout ret = new VerticalLayout();
		ret.setSpacing(true);
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
		if (CredentialResetStateVariable.get() != 2)
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
		CredentialResetFinalDialog dialogFinal = new CredentialResetFinalDialog(msg, backend, credEditor);
		dialogFinal.show();
	}
}
