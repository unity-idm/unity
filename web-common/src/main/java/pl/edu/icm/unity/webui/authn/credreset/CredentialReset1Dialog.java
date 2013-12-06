/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset;

import com.vaadin.server.UserError;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.authn.CredentialReset;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CaptchaComponent;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * Bootstraps credential reset pipeline.
 * @author K. Benedyczak
 */
public class CredentialReset1Dialog extends AbstractDialog
{
	private UnityMessageSource msg;
	private CredentialReset backend;
	private CredentialEditor credEditor;
	
	private TextField username;
	private CaptchaComponent captcha;
	
	public CredentialReset1Dialog(UnityMessageSource msg, CredentialReset backend, CredentialEditor credEditor)
	{
		super(msg, msg.getMessage("CredentialReset.title"), msg.getMessage("CredentialReset.requestReset"),
				msg.getMessage("cancel"));
		this.msg = msg;
		this.defaultSizeUndfined = true;
		this.backend = backend;
		this.credEditor = credEditor;
	}

	@Override
	protected Component getContents() throws Exception
	{
		if (CredentialResetStateVariable.get() != 0)
		{
			ErrorPopup.showError(msg, msg.getMessage("error"),
					msg.getMessage("CredentialReset.illegalAppState"));
			throw new Exception();
		}
		VerticalLayout ret = new VerticalLayout();
		ret.setSpacing(true);
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
		if (backend.getSettings().isRequireSecurityQuestion())
		{
			CredentialReset2Dialog dialog2 = new CredentialReset2Dialog(msg, backend, credEditor, user);
			dialog2.show();
		} else
		{
			//nothing more required, jump to step 3 
			CredentialResetStateVariable.inc();
			CredentialReset3Dialog dialog3 = new CredentialReset3Dialog(msg, backend, credEditor, user);
			dialog3.show();
		}
	}
	
}
