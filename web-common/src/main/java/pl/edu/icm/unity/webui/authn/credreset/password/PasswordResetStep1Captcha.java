/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset.password;

import java.util.function.Consumer;

import com.vaadin.server.UserError;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetFlowConfig;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetLayout;
import pl.edu.icm.unity.webui.authn.credreset.TextFieldWithContextLabel;
import pl.edu.icm.unity.webui.common.CaptchaComponent;

/**
 * Bootstraps credential reset pipeline: asks about username and captcha.
 * 
 * @author K. Benedyczak
 */
class PasswordResetStep1Captcha extends CredentialResetLayout
{
	private UnityMessageSource msg;
	private Runnable cancelCallback;
	private Consumer<String> proceedCallback;
	
	private TextFieldWithContextLabel username;
	private CaptchaComponent captcha;
	private boolean compactLayout;
	
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
		username = new TextFieldWithContextLabel(compactLayout);
		username.setLabel(msg.getMessage("CredentialReset.username"));
		username.setWidth(100, Unit.PERCENTAGE);
		username.focus();
		narrowCol.addComponent(username);
		
		captcha = new CaptchaComponent(msg, compactLayout);
		Component captchaComp = captcha.getAsFullWidthComponent();
		captchaComp.addStyleName("u-credreset-captcha");
		narrowCol.addComponent(captchaComp);
		
		Component buttons = getButtonsBar(msg.getMessage("CredentialReset.requestPasswordReset"), 
				this::onConfirm, msg.getMessage("cancel"), cancelCallback);
		
		narrowCol.addComponent(buttons);
		narrowCol.setComponentAlignment(buttons, Alignment.TOP_CENTER);
		
		Label extraInfo = new Label(msg.getMessage("CredentialReset.captchaExtraInfoHTML"), ContentMode.HTML);
		extraInfo.setWidth(100, Unit.PERCENTAGE);
		extraInfo.addStyleName("u-credreset-resetExtraInfo");
		narrowCol.addComponent(extraInfo);
		return narrowCol;
	}

	private void onConfirm()
	{
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

		proceedCallback.accept(user);
	}
}
