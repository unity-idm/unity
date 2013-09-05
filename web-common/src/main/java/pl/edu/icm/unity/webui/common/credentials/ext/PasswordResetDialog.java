/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.ext;

import com.vaadin.server.UserError;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.CredentialResetSettings;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CaptchaComponent;
import pl.edu.icm.unity.webui.common.ErrorPopup;

/**
 * Bootstraps password reset pipeline.
 * @author K. Benedyczak
 */
public class PasswordResetDialog extends AbstractDialog
{
	private UnityMessageSource msg;
	private CredentialResetSettings settings;
	
	private TextField username;
	private CaptchaComponent captcha;
	
	public PasswordResetDialog(UnityMessageSource msg, CredentialResetSettings settings)
	{
		super(msg, msg.getMessage("PasswordReset.title"), msg.getMessage("PasswordReset.requestReset"),
				msg.getMessage("cancel"));
		this.msg = msg;
		this.defaultSizeUndfined = true;
		this.settings = settings;
	}

	@Override
	protected Component getContents() throws Exception
	{
		if (PasswordResetStateVariable.get() != 0)
		{
			ErrorPopup.showError(msg.getMessage("error"),
					msg.getMessage("PasswordReset.illegalAppState"));
			throw new Exception();
		}
		VerticalLayout ret = new VerticalLayout();
		ret.setSpacing(true);
		ret.addComponent(new Label(msg.getMessage("PasswordReset.info")));
		FormLayout form = new FormLayout();
		username = new TextField(msg.getMessage("PasswordReset.username"));
		captcha = new CaptchaComponent(msg);
		form.addComponent(username);
		captcha.addToFormLayout(form);
		ret.addComponent(form);
		return ret;
	}

	@Override
	protected void onConfirm()
	{
		if (username.getValue() == null || username.getValue().equals(""))
		{
			username.setComponentError(new UserError(msg.getMessage("fieldRequired")));
			return;
		}
		username.setComponentError(null);
		try
		{
			captcha.verify();
		} catch (WrongArgumentException e)
		{
			return;
		}

		close();

		PasswordResetStateVariable.inc(); //ok - username (maybe invalid but anyway) and captcha are provided.
		//in future here we can also go to the 2nd dialog if other attributes are required.
		if (settings.isRequireSecurityQuestion())
			//TODO - go to 2nd step
			;
		else
		{
			//nothing more required, jump to step 3 
			PasswordResetStateVariable.inc();
			//TODO - go to step 3
		}
	}
	
}
