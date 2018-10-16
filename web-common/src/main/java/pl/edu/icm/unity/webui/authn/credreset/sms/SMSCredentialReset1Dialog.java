/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset.sms;

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
import pl.edu.icm.unity.stdext.credential.sms.SMSCredentialRecoverySettings;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetStateVariable;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CaptchaComponent;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * Bootstraps sms credential reset pipeline.
 * @author P. Piernik
 */
public class SMSCredentialReset1Dialog extends AbstractDialog
{
	private UnityMessageSource msg;
	private CredentialReset backend;
	private CredentialEditor credEditor;
	private SMSCredentialRecoverySettings settings;
	
	private TextField username;
	private CaptchaComponent captcha;
	
	public SMSCredentialReset1Dialog(UnityMessageSource msg, CredentialReset backend, CredentialEditor credEditor)
	{
		super(msg, msg.getMessage("CredentialReset.title"), msg.getMessage("CredentialReset.requestReset"),
				msg.getMessage("cancel"));
		this.msg = msg;
		this.backend = backend;
		this.credEditor = credEditor;
		this.settings = new SMSCredentialRecoverySettings(JsonUtil.parse(backend.getSettings()));
		setSizeEm(40, 30);
	}

	@Override
	protected Component getContents() throws Exception
	{
		addStyleName("u-credreset-dialog");

		if (CredentialResetStateVariable.get() != 0)
		{
			NotificationPopup.showError(msg.getMessage("error"),
					msg.getMessage("CredentialReset.illegalAppState"));
			throw new Exception();
		}
		VerticalLayout ret = new VerticalLayout();
		ret.setMargin(false);
		Label info = new Label(msg.getMessage("CredentialReset.info"));
		ret.addComponent(info);
		info.setWidth(100, Unit.PERCENTAGE);
		FormLayout form = new FormLayout();
		username = new TextField(msg.getMessage("CredentialReset.username"));
		captcha = new CaptchaComponent(msg);
		form.addComponent(username);
		if (settings.isCapchaRequire())
		{
			captcha.addToFormLayout(form);
		}
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
			throw new IllegalStateException("Wrong application security state in sms credential reset!" +
					" This should never happen.");
		String user = username.getValue();
		if (user == null || user.equals(""))
		{
			username.setComponentError(new UserError(msg.getMessage("fieldRequired")));
			return;
		}
		username.setComponentError(null);
		if (settings.isCapchaRequire())
		{
			try
			{
				captcha.verify();
			} catch (WrongArgumentException e)
			{
				return;
			}
		}

		close();
		backend.setSubject(new IdentityTaV(UsernameIdentity.ID, user));
		CredentialResetStateVariable.inc(); //ok - username (maybe invalid but anyway) and captcha are provided.

		EmailCodeSMSCredentialResetDialog dialog2 = new EmailCodeSMSCredentialResetDialog(msg, backend,
				credEditor, user);
		dialog2.show();
	}
	
}
