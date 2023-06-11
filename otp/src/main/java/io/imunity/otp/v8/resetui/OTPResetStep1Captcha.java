/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp.v8.resetui;

import java.util.function.Consumer;

import com.vaadin.server.UserError;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetFlowConfig;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetLayout;
import pl.edu.icm.unity.webui.authn.credreset.TextFieldWithContextLabel;
import pl.edu.icm.unity.webui.common.CaptchaComponent;

/**
 * Bootstraps OTP credential reset pipeline: captcha and username.
 */
class OTPResetStep1Captcha extends CredentialResetLayout
{
	private MessageSource msg;
	
	private TextFieldWithContextLabel username;
	private CaptchaComponent captcha;
	private Consumer<String> proceedCallback;
	private Runnable cancelCallback;

	private boolean compactLayout;

	private boolean collectUsername;
	
	OTPResetStep1Captcha(CredentialResetFlowConfig credResetConfig, boolean collectUsername, Consumer<String> proceedCallback)
	{
		super(credResetConfig);
		this.collectUsername = collectUsername;
		this.msg = credResetConfig.msg;
		this.proceedCallback = proceedCallback;
		this.cancelCallback = credResetConfig.cancelCallback;
		compactLayout = credResetConfig.compactLayout;
		initUI(msg.getMessage("OTPCredentialReset.info"), getContents());
	}

	private Component getContents()
	{
		VerticalLayout narrowCol = new VerticalLayout();
		narrowCol.setMargin(false);
		narrowCol.setWidth(MAIN_WIDTH_EM, Unit.EM);
		username = new TextFieldWithContextLabel(compactLayout);
		username.setLabel(msg.getMessage("CredentialReset.username"));
		username.setWidth(100, Unit.PERCENTAGE);
		captcha = new CaptchaComponent(msg, compactLayout);
		narrowCol.addComponent(username);

		Component captchaComp = captcha.getAsFullWidthComponent();
		captchaComp.addStyleName("u-credreset-captcha");
		narrowCol.addComponent(captchaComp);
		
		Component buttons = getButtonsBar(msg.getMessage("OTPCredentialReset.requestReset"), 
				this::onConfirm, msg.getMessage("cancel"), cancelCallback);
		
		narrowCol.addComponent(buttons);
		narrowCol.setComponentAlignment(buttons, Alignment.TOP_CENTER);
		
		if (!collectUsername)
			username.setVisible(false);
		return narrowCol;
	}

	private void onConfirm()
	{
		String user = null;
		if (collectUsername)
		{
			user = username.getValue();
			if (user == null || user.equals(""))
			{
				username.setComponentError(new UserError(msg.getMessage("fieldRequired")));
				return;
			}
			username.setComponentError(null);
		}
		try
		{
			captcha.verify();
		} catch (WrongArgumentException e)
		{
			return;
		}
		proceedCallback.accept(user);
	}
}
