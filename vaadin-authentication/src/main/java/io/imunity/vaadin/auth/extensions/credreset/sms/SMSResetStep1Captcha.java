/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.extensions.credreset.sms;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetFlowConfig;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetLayout;
import io.imunity.vaadin.auth.extensions.credreset.TextFieldWithContextLabel;
import io.imunity.vaadin.endpoint.common.forms.components.CaptchaComponent;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.WrongArgumentException;

import java.util.function.Consumer;

/**
 * Bootstraps sms credential reset pipeline.
 */
public class SMSResetStep1Captcha extends CredentialResetLayout
{
	private final MessageSource msg;
	
	private TextFieldWithContextLabel username;
	private CaptchaComponent captcha;
	private final Consumer<String> proceedCallback;
	private final Runnable cancelCallback;

	private final boolean requireCaptcha;

	private final boolean compactLayout;

	private final boolean collectUsername;
	
	public SMSResetStep1Captcha(CredentialResetFlowConfig credResetConfig, boolean requireCaptcha,
	                            Consumer<String> proceedCallback, boolean collectUsername)
	{
		super(credResetConfig);
		this.collectUsername = collectUsername;
		this.msg = credResetConfig.msg;
		this.requireCaptcha = requireCaptcha;
		this.proceedCallback = proceedCallback;
		this.cancelCallback = credResetConfig.cancelCallback;
		compactLayout = credResetConfig.compactLayout;
		initUI(msg.getMessage("CredentialReset.infoMobile"), getContents());
	}

	private Component getContents()
	{
		VerticalLayout narrowCol = new VerticalLayout();
		narrowCol.setMargin(false);
		narrowCol.setPadding(false);
		narrowCol.setWidth(MAIN_WIDTH_EM, Unit.EM);
		username = new TextFieldWithContextLabel(compactLayout);
		username.setLabel(msg.getMessage("CredentialReset.username"));
		username.setWidthFull();
		captcha = new CaptchaComponent(msg, 6, compactLayout);
		narrowCol.add(username);
		if (requireCaptcha)
		{
			VerticalLayout captchaComp = captcha.getAsComponent();
			captchaComp.addClassName("u-credreset-captcha");
			narrowCol.add(captchaComp);
		}
		
		Component buttons = getButtonsBar(msg.getMessage("CredentialReset.requestMobileReset"), 
				this::onConfirm, msg.getMessage("cancel"), cancelCallback);
		
		narrowCol.add(buttons);
		narrowCol.setAlignItems(Alignment.CENTER);
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
				username.setInvalid(true);
				username.setErrorMessage(msg.getMessage("fieldRequired"));
				return;
			}
			username.setInvalid(false);
		}
		if (requireCaptcha)
		{
			try
			{
				captcha.verify();
			} catch (WrongArgumentException e)
			{
				return;
			}
		}
		proceedCallback.accept(user);
	}
}
