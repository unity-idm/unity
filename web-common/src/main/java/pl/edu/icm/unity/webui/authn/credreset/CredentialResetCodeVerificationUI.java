/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.credreset;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.UserError;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.TooManyAttempts;
import pl.edu.icm.unity.webui.authn.credreset.password.PasswordCredentialResetController.CodeConsumer;
import pl.edu.icm.unity.webui.authn.credreset.password.PasswordCredentialResetController.CodeSender;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * On this UI the user must provide the reset code which was sent to it. Universal can be used for 
 * email and SMS sent codes.
 * 
 * @author P.Piernik
 *
 */
public class CredentialResetCodeVerificationUI extends CredentialResetLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, CredentialResetCodeVerificationUI.class);
	private Runnable cancelCallback;
	private CodeConsumer proceedCallback;
	private CodeSender codeSendCallback;
	
	private TextFieldWithContextLabel answer;
	private UnityMessageSource msg;
	private CredentialResetFlowConfig credResetConfig;
	
	public CredentialResetCodeVerificationUI(CredentialResetFlowConfig credResetConfig, CodeConsumer proceedCallback, 
			CodeSender codeSender, String title, String answerCaption, String resendDesc)
	{
		super(credResetConfig);
		this.credResetConfig = credResetConfig;
		this.msg = credResetConfig.msg;
		this.proceedCallback = proceedCallback;
		this.codeSendCallback = codeSender;
		this.cancelCallback = credResetConfig.cancelCallback;
		initUI(title, getContents(answerCaption, resendDesc));
	}

	private Component getContents(String answerCaption, String resendDesc)
	{
		answer = new TextFieldWithContextLabel(credResetConfig.compactLayout);
		answer.setLabel(answerCaption);
		answer.setWidth(100, Unit.PERCENTAGE);
		answer.focus();
		VerticalLayout form = new VerticalLayout(answer);
		form.setMargin(false);
		form.setComponentAlignment(answer, Alignment.TOP_CENTER);
		
		Component buttons = getButtonsBar(msg.getMessage("continue"), this::onConfirm, 
				msg.getMessage("cancel"), cancelCallback);

		Component resend = getResendComponent(resendDesc);
		VerticalLayout narrowCol = new VerticalLayout();
		narrowCol.setWidth(MAIN_WIDTH_EM, Unit.EM);
		narrowCol.setMargin(false);
		narrowCol.addComponents(form, buttons, resend);
		narrowCol.setComponentAlignment(resend, Alignment.TOP_RIGHT);
		narrowCol.setComponentAlignment(buttons, Alignment.TOP_CENTER);
		return narrowCol;
	}

	private Component getResendComponent(String resendDesc)
	{
		CssLayout wrapper = new CssLayout(); 
		Button resend = new Button(msg.getMessage("CredentialReset.resend"));
		wrapper.addStyleName(Styles.textCenter.toString());
		wrapper.addStyleName("u-credreset-resend");
		resend.addStyleName(Styles.vButtonLink.toString());
		resend.addStyleName("u-credreset-resend-button");
		resend.setDescription(resendDesc);
		resend.addClickListener(event ->
		{
			try
			{
				codeSendCallback.resendCode();
			} catch (TooManyAttempts e)
			{
				resend.setEnabled(false);
			}
		});
		
		Label resendPfx = new Label(msg.getMessage("CredentialReset.resendPrefix"));
		Label resendSuffix = new Label(msg.getMessage("CredentialReset.resendSuffix"));
		wrapper.addComponents(resendPfx, resend, resendSuffix);
		return wrapper;
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
			proceedCallback.acceptCode(answer.getValue());
		} catch (TooManyAttempts e)
		{
			NotificationPopup.showError(msg.getMessage("CredentialReset.codeInvalidOrExpired"), "");
			cancelCallback.run();
		} catch (Exception e)
		{
			log.debug("Wrong code received", e);
			answer.setValue("");
			NotificationPopup.showError(msg.getMessage("CredentialReset.codeInvalid"), "");
			answer.focus();
		}
	}
}
