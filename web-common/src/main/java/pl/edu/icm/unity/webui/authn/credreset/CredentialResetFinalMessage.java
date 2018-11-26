/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

/**
 * Confirmation message post successful credential reset
 * 
 * @author K. Benedyczak
 */
public class CredentialResetFinalMessage extends CredentialResetLayout
{
	private UnityMessageSource msg;
	private Runnable closeCallback;
	
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
		ret.setSpacing(true);
		ret.setWidth(MAIN_WIDTH_EM, Unit.EM);

		Button proceed = new Button(msg.getMessage("continue"));
		proceed.addStyleName("u-cred-reset-proceed");
		proceed.addClickListener(e -> closeCallback.run());
		proceed.setWidth(100, Unit.PERCENTAGE);
		proceed.setClickShortcut(KeyCode.ENTER);

		ret.addComponent(proceed);
		
		return ret;
	}
}
