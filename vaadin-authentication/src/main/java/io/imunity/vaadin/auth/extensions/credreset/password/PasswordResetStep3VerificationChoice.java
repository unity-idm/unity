/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.extensions.credreset.password;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetFlowConfig;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetLayout;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.function.Consumer;

/**
 * 3rd step of credential reset pipeline. On this screen user can choose verification method - email or mobile.
 */
class PasswordResetStep3VerificationChoice extends CredentialResetLayout
{	
	private ComboBox<PasswordCredentialResetController.VerificationMethod> chooser;
	private final MessageSource msg;
	private final Runnable cancelCallback;
	private final Consumer<PasswordCredentialResetController.VerificationMethod> proceedCallback;
	
	PasswordResetStep3VerificationChoice(CredentialResetFlowConfig credResetConfig,
	                                     Consumer<PasswordCredentialResetController.VerificationMethod> proceedCallback)
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
		chooser.setLabel(msg.getMessage("CredentialReset.chooseVerificationMethod"));
		chooser.setItems(PasswordCredentialResetController.VerificationMethod.values());
		chooser.setValue(PasswordCredentialResetController.VerificationMethod.Email);
		chooser.setItemLabelGenerator(i -> msg.getMessage("CredentialReset." + i.toString()));
		chooser.setWidthFull();
		chooser.focus();
		
		VerticalLayout ret = new VerticalLayout(chooser);
		ret.setMargin(false);
		ret.setPadding(false);
		ret.setAlignItems(Alignment.CENTER);
		
		Component buttons = getButtonsBar(msg.getMessage("continue"), this::onConfirm, 
				msg.getMessage("cancel"), cancelCallback);
		
		VerticalLayout narrowCol = new VerticalLayout();
		narrowCol.setWidth(MAIN_WIDTH_EM, Unit.EM);
		narrowCol.setMargin(false);
		narrowCol.add(chooser, buttons);
		narrowCol.setAlignItems(Alignment.CENTER);
		return narrowCol;
	}

	private void onConfirm()
	{
		proceedCallback.accept(chooser.getValue());
	}
}
