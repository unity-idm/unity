/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.credreset.password;

import java.util.function.Consumer;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetFlowConfig;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetLayout;
import pl.edu.icm.unity.webui.authn.credreset.password.PasswordCredentialResetController.VerificationMethod;

/**
 * 3rd step of credential reset pipeline. On this screen user can choose verification method - email or mobile.
 *  
 * @author P.Piernik
 */
class PasswordResetStep3VerificationChoice extends CredentialResetLayout
{	
	private ComboBox<VerificationMethod> chooser;
	private UnityMessageSource msg;
	private Runnable cancelCallback;
	private Consumer<VerificationMethod> proceedCallback;
	
	PasswordResetStep3VerificationChoice(CredentialResetFlowConfig credResetConfig,  
			Consumer<VerificationMethod> proceedCallback)
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
		chooser.setItems(VerificationMethod.values());
		chooser.setValue(VerificationMethod.Email);
		chooser.setEmptySelectionAllowed(false);	
		chooser.setItemCaptionGenerator(i -> msg.getMessage("CredentialReset." + i.toString()));
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
