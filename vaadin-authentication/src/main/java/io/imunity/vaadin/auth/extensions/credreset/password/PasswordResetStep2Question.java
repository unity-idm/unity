/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.extensions.credreset.password;


import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.auth.extensions.credreset.AnswerConsumer;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetFlowConfig;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetLayout;
import io.imunity.vaadin.auth.extensions.credreset.TextFieldWithContextLabel;
import io.imunity.vaadin.elements.NotificationPresenter;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.TooManyAttempts;

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
 */
class PasswordResetStep2Question extends CredentialResetLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, PasswordResetStep2Question.class);
	private final MessageSource msg;
	private final Runnable cancelCallback;
	private final AnswerConsumer proceedCallback;
	private final NotificationPresenter notificationPresenter;
	private TextFieldWithContextLabel answer;
	private final CredentialResetFlowConfig credResetConfig;
	
	PasswordResetStep2Question(CredentialResetFlowConfig credResetConfig, String question,
	                           AnswerConsumer proceedCallback, NotificationPresenter notificationPresenter)
	{
		super(credResetConfig);
		this.credResetConfig = credResetConfig;
		this.msg = credResetConfig.msg;
		this.proceedCallback = proceedCallback;
		this.cancelCallback = credResetConfig.cancelCallback;
		this.notificationPresenter = notificationPresenter;

		initUI(question, getContents());
	}

	private Component getContents()
	{
		answer = new TextFieldWithContextLabel(credResetConfig.compactLayout);
		answer.setLabel(msg.getMessage("CredentialReset.answer"));
		answer.setWidthFull();
		answer.focus();
		
		Component buttons = getButtonsBar(msg.getMessage("continue"), this::onConfirm, 
				msg.getMessage("cancel"), this::onCancel);
		
		VerticalLayout narrowCol = new VerticalLayout();
		narrowCol.setWidth(MAIN_WIDTH_EM, Unit.EM);
		narrowCol.setMargin(false);
		narrowCol.setPadding(false);
		VerticalLayout form = new VerticalLayout(answer);
		form.setMargin(false);
		form.setPadding(false);
		narrowCol.add(form);
		
		narrowCol.add(buttons);
		narrowCol.setAlignItems(Alignment.CENTER);
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
			answer.setInvalid(true);
			answer.setErrorMessage(msg.getMessage("fieldRequired"));
			return;
		}
		answer.setInvalid(false);
		try
		{
			proceedCallback.acceptAnswer(a);
		} catch (TooManyAttempts e) 
		{
			notificationPresenter.showError(msg.getMessage("error"),
					msg.getMessage("CredentialReset.usernameOrAnswerInvalid"));
			onCancel();
		} catch (Exception e)
		{
			log.debug("Answer checking problem, likely just wrong", e);
			answer.setValue("");
			notificationPresenter.showError(msg.getMessage("error"),
					msg.getMessage("CredentialReset.usernameOrAnswerInvalid"));
		}
	}
}
