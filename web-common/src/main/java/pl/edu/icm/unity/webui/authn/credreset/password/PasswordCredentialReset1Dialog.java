/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset.password;

import com.vaadin.server.UserError;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.credential.PasswordCredentialResetSettings;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetStateVariable;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CaptchaComponent;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * Bootstraps credential reset pipeline.
 * @author K. Benedyczak
 */
public class PasswordCredentialReset1Dialog extends AbstractDialog
{
	private UnityMessageSource msg;
	private CredentialReset backend;
	private CredentialEditor credEditor;
	
	private TextField username;
	private CaptchaComponent captcha;
	private PasswordCredentialResetSettings settings;
	
	public PasswordCredentialReset1Dialog(UnityMessageSource msg, CredentialReset backend, CredentialEditor credEditor)
	{
		super(msg, msg.getMessage("CredentialReset.title"), msg.getMessage("CredentialReset.requestReset"),
				msg.getMessage("cancel"));
		this.msg = msg;
		this.backend = backend;
		this.credEditor = credEditor;
		this.settings = new PasswordCredentialResetSettings(JsonUtil.parse(backend.getSettings()));
	}

	@Override
	protected Component getContents() throws Exception
	{
		if (CredentialResetStateVariable.get() != 0)
		{
			NotificationPopup.showError(msg, msg.getMessage("error"),
					msg.getMessage("CredentialReset.illegalAppState"));
			throw new Exception();
		}
		VerticalLayout ret = new VerticalLayout();
		ret.setMargin(false);
		ret.addComponent(new Label(msg.getMessage("CredentialReset.info")));
		FormLayout form = new FormLayout();
		username = new TextField(msg.getMessage("CredentialReset.username"));
		captcha = new CaptchaComponent(msg);
		form.addComponent(username);
		captcha.addToFormLayout(form);
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
		if (CredentialResetStateVariable.get() != 0)
			throw new IllegalStateException("Wrong application security state in password reset!" +
					" This should never happen.");
		String user = username.getValue();
		if (user == null || user.equals(""))
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
		backend.setSubject(new IdentityTaV(UsernameIdentity.ID, user));
		CredentialResetStateVariable.inc(); //ok - username (maybe invalid but anyway) and captcha are provided.
		//in future here we can also go to the 2nd dialog if other attributes are required.		
		if (settings.isRequireSecurityQuestion())
		{
			PasswordCredentialReset2Dialog dialog2 = new PasswordCredentialReset2Dialog(msg, backend,
					credEditor, user);
			dialog2.show();
		} else if (settings.isRequireEmailConfirmation())
		{
			CredentialResetStateVariable.inc();
			CredentialResetStateVariable.inc();
			EmailCodePasswordCredentialReset4Dialog dialog4 = new EmailCodePasswordCredentialReset4Dialog(
					msg, backend, credEditor, user);
			dialog4.show();
		} else if (settings.isRequireMobileConfirmation())

		{
			CredentialResetStateVariable.inc();
			CredentialResetStateVariable.inc();
			CredentialResetStateVariable.inc();
			MobileCodePasswordCredentialReset5Dialog dialog5 = new MobileCodePasswordCredentialReset5Dialog(
					msg, backend, credEditor, user);
			dialog5.show();

		} else

		{
			CredentialResetStateVariable.inc();
			PasswordCredentialResetVerificationChoose3Dialog dialog3 = new PasswordCredentialResetVerificationChoose3Dialog(
					msg, backend, credEditor, user);
			dialog3.show();
		}
	}
	
}
