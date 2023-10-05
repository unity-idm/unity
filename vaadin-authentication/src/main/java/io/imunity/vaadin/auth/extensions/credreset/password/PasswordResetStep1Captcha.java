/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.extensions.credreset.password;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetFlowConfig;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetLayout;
import io.imunity.vaadin.auth.extensions.credreset.TextFieldWithContextLabel;
import io.imunity.vaadin.endpoint.common.forms.components.CaptchaComponent;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.function.Consumer;

/**
 * Bootstraps credential reset pipeline: asks about username and captcha.
 */
class PasswordResetStep1Captcha extends CredentialResetLayout
{
	private final MessageSource msg;
	private final Runnable cancelCallback;
	private final Consumer<String> proceedCallback;
	
	private TextFieldWithContextLabel username;
	private CaptchaComponent captcha;
	private final boolean compactLayout;
	
	PasswordResetStep1Captcha(CredentialResetFlowConfig credResetConfig, Consumer<String> proceedCallback)
	{
		super(credResetConfig);
		this.proceedCallback = proceedCallback;
		this.cancelCallback = credResetConfig.cancelCallback;
		this.msg = credResetConfig.msg;
		compactLayout = credResetConfig.compactLayout;
		initUI(msg.getMessage("CredentialReset.infoPassword"), getContents());
	}

	private Component getContents()
	{
		VerticalLayout narrowCol = new VerticalLayout();
		narrowCol.setWidth(MAIN_WIDTH_EM, Unit.EM);
		narrowCol.setMargin(false);
		narrowCol.setPadding(false);
		username = new TextFieldWithContextLabel(compactLayout);
		username.setLabel(msg.getMessage("CredentialReset.username"));
		username.setWidthFull();
		username.focus();
		narrowCol.add(username);
		
		captcha = new CaptchaComponent(msg, 6, compactLayout);
		VerticalLayout captchaComp = captcha.getAsComponent();
		captchaComp.addClassName("u-credreset-captcha");
		narrowCol.add(captchaComp);
		
		Component buttons = getButtonsBar(msg.getMessage("CredentialReset.requestPasswordReset"), 
				this::onConfirm, msg.getMessage("cancel"), cancelCallback);
		
		narrowCol.add(buttons);
		narrowCol.setAlignItems(Alignment.CENTER);

		Span extraInfo = new Span(msg.getMessage("CredentialReset.captchaExtraInfoHTML"));
		extraInfo.setWidth(100, Unit.PERCENTAGE);
		extraInfo.addClassName("u-credreset-resetExtraInfo");
		narrowCol.add(extraInfo);
		return narrowCol;
	}

	private void onConfirm()
	{
		String user = username.getValue();
		if (user == null || user.equals(""))
		{
			username.setInvalid(true);
			username.setErrorMessage(msg.getMessage("fieldRequired"));
			return;
		}
		username.setInvalid(false);
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
