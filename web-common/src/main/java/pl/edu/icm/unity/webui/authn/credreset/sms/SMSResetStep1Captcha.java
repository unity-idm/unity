/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset.sms;

import java.util.function.Consumer;

import com.vaadin.server.UserError;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetFlowConfig;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetLayout;
import pl.edu.icm.unity.webui.authn.credreset.TextFieldWithContextLabel;
import pl.edu.icm.unity.webui.common.CaptchaComponent;

/**
 * Bootstraps sms credential reset pipeline.
 * 
 * @author P. Piernik
 */
public class SMSResetStep1Captcha extends CredentialResetLayout
{
	private UnityMessageSource msg;
	
	private TextFieldWithContextLabel username;
	private CaptchaComponent captcha;
	private Consumer<String> proceedCallback;
	private Runnable cancelCallback;

	private boolean requireCaptcha;

	private boolean compactLayout;
	
	public SMSResetStep1Captcha(CredentialResetFlowConfig credResetConfig, boolean requireCaptcha, 
			Consumer<String> proceedCallback)
	{
		super(credResetConfig);
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
		narrowCol.setWidth(MAIN_WIDTH_EM, Unit.EM);
		username = new TextFieldWithContextLabel(compactLayout);
		username.setLabel(msg.getMessage("CredentialReset.username"));
		username.setWidth(100, Unit.PERCENTAGE);
		captcha = new CaptchaComponent(msg, compactLayout);
		narrowCol.addComponent(username);
		if (requireCaptcha)
		{
			Component captchaComp = captcha.getAsFullWidthComponent();
			captchaComp.addStyleName("u-credreset-captcha");
			narrowCol.addComponent(captchaComp);
		}
		
		Component buttons = getButtonsBar(msg.getMessage("CredentialReset.requestMobileReset"), 
				this::onConfirm, msg.getMessage("cancel"), cancelCallback);
		
		narrowCol.addComponent(buttons);
		narrowCol.setComponentAlignment(buttons, Alignment.TOP_CENTER);
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
