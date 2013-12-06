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

import pl.edu.icm.unity.exceptions.TooManyAttempts;
import pl.edu.icm.unity.server.authn.CredentialReset;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * 2nd step of credential reset pipeline. In this dialog the user must provide an answet to the security question.
 * In future other attributes might be queried here.
 * <p>
 * This dialog fails if either username or the answer is wrong. This is done to make guessing usernames 
 * more difficult. In future, with other attribute queries it will be even more bullet proof.
 * In case the user is invalid, we present a 'random' question. However we must be sure that for the given 
 * username always the same question is asked, so our choices are not random.
 * <p>
 * This check is intended before any confirmation code sending, not to spam users.
 * 
 * @author K. Benedyczak
 */
public class CredentialReset2Dialog extends AbstractDialog
{
	private UnityMessageSource msg;
	private CredentialReset backend;
	private String username;
	private CredentialEditor credEditor;
	
	private TextField answer;
	
	public CredentialReset2Dialog(UnityMessageSource msg, CredentialReset backend, CredentialEditor credEditor, 
			String username)
	{
		super(msg, msg.getMessage("CredentialReset.title"), msg.getMessage("continue"),
				msg.getMessage("cancel"));
		this.msg = msg;
		this.defaultSizeUndfined = true;
		this.backend = backend;
		this.username = username;
		this.credEditor = credEditor;
	}

	@Override
	protected Component getContents() throws Exception
	{
		if (CredentialResetStateVariable.get() != 1)
		{
			ErrorPopup.showError(msg, msg.getMessage("error"),
					msg.getMessage("CredentialReset.illegalAppState"));
			throw new Exception();
		}
		Label userLabel = new Label(msg.getMessage("CredentialReset.changingFor", username));
		
		Label question = new Label(backend.getSecurityQuestion());
		question.setCaption(msg.getMessage("CredentialReset.question"));
		answer = new TextField(msg.getMessage("CredentialReset.answer"));
		
		VerticalLayout ret = new VerticalLayout();
		ret.setSpacing(true);
		ret.addComponent(userLabel);
		FormLayout form = new FormLayout(question, answer);
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
		if (CredentialResetStateVariable.get() != 1)
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
			backend.verifyStaticData(a);
		} catch (TooManyAttempts e) 
		{
			ErrorPopup.showError(msg, msg.getMessage("error"), 
					msg.getMessage("CredentialReset.usernameOrAnswerInvalid"));
			onCancel();
			return;
		} catch (Exception e)
		{
			answer.setValue("");
			ErrorPopup.showError(msg, msg.getMessage("error"), 
					msg.getMessage("CredentialReset.usernameOrAnswerInvalid"));
			return;
		}
		
		close();

		CredentialResetStateVariable.inc(); //ok - next step allowed
		if (backend.getSettings().isRequireEmailConfirmation())
		{
			CredentialReset3Dialog dialog3 = new CredentialReset3Dialog(msg, backend, credEditor, username);
			dialog3.show();
		} else
		{
			//nothing more required, jump to final step 4 
			CredentialResetStateVariable.inc();
			CredentialResetFinalDialog dialogFinal = new CredentialResetFinalDialog(msg, backend, credEditor);
			dialogFinal.show();
		}
	}
	
}
