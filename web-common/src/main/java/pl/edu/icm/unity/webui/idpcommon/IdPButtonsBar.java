/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.idpcommon;

import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;

/**
 * Bar with buttons: Accept, Decline, Login as another user. The login as another user action is 
 * typically handled internally and needs no additional intervention. 
 * @author K. Benedyczak
 */
public class IdPButtonsBar extends CustomComponent
{
	public enum Action {ACCEPT, DENY, LOGIN_AS_ANOTHER};

	private UnityMessageSource msg;
	private StandardWebAuthenticationProcessor authnProcessor;
	private ActionListener listener;
	
	public IdPButtonsBar(UnityMessageSource msg, StandardWebAuthenticationProcessor authnProcessor,
			ActionListener listener)
	{
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.listener = listener;
		initUI();
	}

	private void initUI()
	{
		setSizeUndefined();
		HorizontalLayout buttons = new HorizontalLayout();
		
		Button confirmB = new Button(msg.getMessage("IdPButtonsBar.confirm"));
		confirmB.setId("IdpButtonsBar.confirmButton");
		confirmB.addClickListener(event -> listener.buttonClicked(Action.ACCEPT));
		
		Button declineB = new Button(msg.getMessage("IdPButtonsBar.decline"));
		declineB.addClickListener(event -> listener.buttonClicked(Action.DENY));
		
		Button reloginB = new Button(msg.getMessage("IdPButtonsBar.logAsAnother"));
		reloginB.addClickListener(event -> 
		{
			listener.buttonClicked(Action.LOGIN_AS_ANOTHER);
			authnProcessor.logout(true);
		});
		
		buttons.addComponents(confirmB, declineB, reloginB);
		buttons.setSizeUndefined();
		setCompositionRoot(buttons);
	}


	public static interface ActionListener
	{
		void buttonClicked(Action accept);
	}
}
