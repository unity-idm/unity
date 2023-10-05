/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.extensions.credreset;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import io.imunity.vaadin.elements.LinkButton;
import io.imunity.vaadin.elements.NotificationPresenter;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.TooManyAttempts;

/**
 * On this UI the user must provide the reset code which was sent to it. Universal can be used for 
 * email and SMS sent codes.
 */
public class CredentialResetCodeVerificationUI extends CredentialResetLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, CredentialResetCodeVerificationUI.class);
	private final MessageSource msg;
	private final CredentialResetFlowConfig credResetConfig;
	private final Runnable cancelCallback;
	private final CodeConsumer proceedCallback;
	private final CodeSender codeSendCallback;
	private final NotificationPresenter notificationPresenter;

	private TextFieldWithContextLabel answer;

	public CredentialResetCodeVerificationUI(CredentialResetFlowConfig credResetConfig, CodeConsumer proceedCallback,
	                                         CodeSender codeSender, String title, String answerCaption, String resendDesc,
	                                         NotificationPresenter notificationPresenter)
	{
		super(credResetConfig);
		this.credResetConfig = credResetConfig;
		this.msg = credResetConfig.msg;
		this.proceedCallback = proceedCallback;
		this.codeSendCallback = codeSender;
		this.cancelCallback = credResetConfig.cancelCallback;
		this.notificationPresenter = notificationPresenter;
		initUI(title, getContents(answerCaption, resendDesc));
	}

	private Component getContents(String answerCaption, String resendDesc)
	{
		answer = new TextFieldWithContextLabel(credResetConfig.compactLayout);
		answer.setLabel(answerCaption);
		answer.setWidthFull();
		answer.focus();
		VerticalLayout form = new VerticalLayout(answer);
		form.setMargin(false);
		form.setPadding(false);
		form.setAlignItems(Alignment.CENTER);
		
		Component buttons = getButtonsBar(msg.getMessage("continue"), this::onConfirm,
				msg.getMessage("cancel"), cancelCallback);

		Component resend = getResendComponent(resendDesc);
		VerticalLayout narrowCol = new VerticalLayout();
		narrowCol.setWidth(MAIN_WIDTH_EM, Unit.EM);
		narrowCol.setMargin(false);
		narrowCol.setPadding(false);
		narrowCol.add(form, buttons, resend);
		narrowCol.setAlignItems(Alignment.CENTER);
		return narrowCol;
	}

	private Component getResendComponent(String resendDesc)
	{
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setMargin(false);
		wrapper.setPadding(false);
		wrapper.setAlignItems(Alignment.END);
		wrapper.getStyle().set("gap", "0");
		LinkButton resend = new LinkButton(msg.getMessage("CredentialReset.resend"), event ->
		{
			try
			{
				codeSendCallback.resendCode();
			} catch (TooManyAttempts e)
			{
				event.getSource().setEnabled(false);
			}
		});
		wrapper.addClassName("u-credreset-resend");
		resend.addClassName("u-credreset-resend-button");
		Tooltip.forComponent(resend).setText(resendDesc);

		Span resendPfx = new Span(msg.getMessage("CredentialReset.resendPrefix"));
		Span resendSuffix = new Span(msg.getMessage("CredentialReset.resendSuffix"));
		wrapper.add(resendPfx, resend, resendSuffix);
		return wrapper;
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
			proceedCallback.acceptCode(answer.getValue());
		} catch (TooManyAttempts e)
		{
			notificationPresenter.showError(msg.getMessage("CredentialReset.codeInvalidOrExpired"), "");
			cancelCallback.run();
		} catch (Exception e)
		{
			log.info("Wrong code received", e);
			answer.setValue("");
			notificationPresenter.showError(msg.getMessage("CredentialReset.codeInvalid"), "");
			answer.focus();
		}
	}
}
