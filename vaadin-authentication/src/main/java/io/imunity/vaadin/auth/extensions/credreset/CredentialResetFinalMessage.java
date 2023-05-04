/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.extensions.credreset;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import pl.edu.icm.unity.MessageSource;


/**
 * Confirmation message post successful credential reset
 */
public class CredentialResetFinalMessage extends CredentialResetLayout
{
	private final MessageSource msg;
	private final Runnable closeCallback;
	
	public CredentialResetFinalMessage(CredentialResetFlowConfig credResetConfig, String message)
	{
		super(credResetConfig);
		this.msg = credResetConfig.msg;
		this.closeCallback = credResetConfig.cancelCallback;
		initUI(message, getContents());
	}

	private Component getContents()
	{
		VerticalLayout ret = new VerticalLayout();
		ret.setMargin(false);
		ret.setPadding(false);
		ret.setWidth(MAIN_WIDTH_EM, Unit.EM);

		Button proceed = new Button(msg.getMessage("continue"));
		proceed.addClassName("u-cred-reset-proceed");
		proceed.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		proceed.addClickListener(e -> closeCallback.run());
		proceed.setWidth(100, Unit.PERCENTAGE);
		proceed.addClickShortcut(com.vaadin.flow.component.Key.ENTER);

		ret.add(proceed);
		
		return ret;
	}
}
