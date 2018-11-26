/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset.password;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.UserError;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.TooManyAttempts;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetFlowConfig;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetLayout;
import pl.edu.icm.unity.webui.authn.credreset.TextFieldWithContextLabel;
import pl.edu.icm.unity.webui.authn.credreset.password.PasswordCredentialResetController.AnswerConsumer;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * 2nd step of credential reset pipeline. On this screen the user must provide an answer to the security question.
 * In future other attributes might be queried here.
 * <p>
 * Fails if either username or the answer is wrong. This is done to make guessing usernames 
 * more difficult. In future, with other attribute queries it will be even more bullet proof.
 * In case the user is invalid, we present a 'random' question. However we must be sure that for the given 
 * username always the same question is asked, so our choices are not random.
 * <p>
 * This check is intended before any confirmation code sending, not to spam users.
 * 
 * @author K. Benedyczak
 */
class PasswordResetStep2Question extends CredentialResetLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, PasswordResetStep2Question.class);
	private UnityMessageSource msg;
	private Runnable cancelCallback;
	private AnswerConsumer proceedCallback;
	private TextFieldWithContextLabel answer;
	private CredentialResetFlowConfig credResetConfig;
	
	PasswordResetStep2Question(CredentialResetFlowConfig credResetConfig, String question, 
			String username, AnswerConsumer proceedCallback)
	{
		super(credResetConfig);
		this.credResetConfig = credResetConfig;
		this.msg = credResetConfig.msg;
		this.proceedCallback = proceedCallback;
		this.cancelCallback = credResetConfig.cancelCallback;

		initUI(question, getContents(username));
	}

	private Component getContents(String username)
	{
		answer = new TextFieldWithContextLabel(credResetConfig.compactLayout);
		answer.setLabel(msg.getMessage("CredentialReset.answer"));
		answer.setWidth(100, Unit.PERCENTAGE);
		answer.focus();
		
		Component buttons = getButtonsBar(msg.getMessage("continue"), this::onConfirm, 
				msg.getMessage("cancel"), this::onCancel);
		
		VerticalLayout narrowCol = new VerticalLayout();
		narrowCol.setWidth(MAIN_WIDTH_EM, Unit.EM);
		narrowCol.setMargin(false);
		VerticalLayout form = new VerticalLayout(answer);
		form.setMargin(false);
		narrowCol.addComponent(form);
		
		narrowCol.addComponent(buttons);
		narrowCol.setComponentAlignment(buttons, Alignment.TOP_CENTER);
		return narrowCol;
	}

	private void onCancel()
	{
		cancelCallback.run();
	}
	
	private void onConfirm()
	{
		String a = answer.getValue();
		if (a == null || a.equals(""))
		{
			answer.setComponentError(new UserError(msg.getMessage("fieldRequired")));
			return;
		}
		answer.setComponentError(null);

		try
		{
			proceedCallback.acceptAnswer(a);
		} catch (TooManyAttempts e) 
		{
			NotificationPopup.showError(msg.getMessage("error"), 
					msg.getMessage("CredentialReset.usernameOrAnswerInvalid"));
			onCancel();
			return;
		} catch (Exception e)
		{
			log.debug("Answer checking problem, likely just wrong", e);
			answer.setValue("");
			NotificationPopup.showError(msg.getMessage("error"), 
					msg.getMessage("CredentialReset.usernameOrAnswerInvalid"));
			return;
		}
	}
}
