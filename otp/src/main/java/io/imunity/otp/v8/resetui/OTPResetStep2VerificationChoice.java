/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp.v8.resetui;

import java.util.function.Consumer;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetFlowConfig;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetLayout;

/**
 * 2nd step of credential reset pipeline. On this screen user can choose verification method - email or mobile.
 */
class OTPResetStep2VerificationChoice extends CredentialResetLayout
{
	private ComboBox<OTPCredentialResetController.VerificationMethod> chooser;
	private MessageSource msg;
	private Runnable cancelCallback;
	private Consumer<OTPCredentialResetController.VerificationMethod> proceedCallback;
	
	OTPResetStep2VerificationChoice(CredentialResetFlowConfig credResetConfig,  
			Consumer<OTPCredentialResetController.VerificationMethod> proceedCallback)
	{
		super(credResetConfig);
		this.msg = credResetConfig.msg;
		this.proceedCallback = proceedCallback;
		this.cancelCallback = credResetConfig.cancelCallback;
		initUI(msg.getMessage("CredentialReset.selectMethodTitle"), getContents());
	}

	private Component getContents()
	{
		chooser = new ComboBox<>();
		chooser.setCaption(msg.getMessage("CredentialReset.chooseVerificationMethod"));
		chooser.setItems(OTPCredentialResetController.VerificationMethod.values());
		chooser.setValue(OTPCredentialResetController.VerificationMethod.EMAIL);
		chooser.setEmptySelectionAllowed(false);	
		chooser.setItemCaptionGenerator(i -> msg.getMessage("OTPCredentialReset.method." + i.toString()));
		chooser.setWidth(100, Unit.PERCENTAGE);
		chooser.focus();
		
		VerticalLayout ret = new VerticalLayout(chooser);
		ret.setMargin(false);
		ret.setComponentAlignment(chooser,  Alignment.TOP_CENTER);
		
		Component buttons = getButtonsBar(msg.getMessage("continue"), this::onConfirm, 
				msg.getMessage("cancel"), cancelCallback);
		
		VerticalLayout narrowCol = new VerticalLayout();
		narrowCol.setWidth(MAIN_WIDTH_EM, Unit.EM);
		narrowCol.setMargin(false);
		narrowCol.addComponents(chooser, buttons);
		narrowCol.setComponentAlignment(buttons, Alignment.TOP_CENTER);
		return narrowCol;
	}

	private void onConfirm()
	{
		proceedCallback.accept(chooser.getValue());
	}
}
