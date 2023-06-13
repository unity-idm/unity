/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.idpcommon;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.authn.StandardWebLogoutHandler;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Bar with buttons: Accept, Decline, Login as another user. The login as another user action is 
 * typically handled internally and needs no additional intervention. 
 * @author K. Benedyczak
 */
public class IdPButtonsBar extends CustomComponent
{
	public enum Action {ACCEPT, DENY, LOGIN_AS_ANOTHER};

	private MessageSource msg;
	private StandardWebLogoutHandler authnProcessor;
	private ActionListener listener;
	private Button confirmB;
	
	public IdPButtonsBar(MessageSource msg, StandardWebLogoutHandler authnProcessor,
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
		VerticalLayout buttonsVL = new VerticalLayout();
		buttonsVL.setMargin(false);
		
		confirmB = new Button(msg.getMessage("IdPButtonsBar.confirm"));
		confirmB.setId("IdpButtonsBar.confirmButton");
		confirmB.addClickListener(this::onAccept);
		confirmB.addStyleName(Styles.vButtonPrimary.toString());
		confirmB.addStyleName("u-consentConfirmButton");
		confirmB.setClickShortcut(KeyCode.ENTER);
		
		Button declineB = new Button(msg.getMessage("IdPButtonsBar.decline"));
		declineB.addClickListener(event -> listener.buttonClicked(Action.DENY));
		declineB.addStyleName("u-consentDeclineButton");
		HorizontalLayout buttonsHL = new HorizontalLayout();
		buttonsHL.setMargin(false);
		buttonsHL.addComponents(declineB, confirmB);
		
		Button reloginB = new Button(msg.getMessage("IdPButtonsBar.logAsAnother"));
		reloginB.addStyleName(Styles.vButtonLink.toString());
		reloginB.addClickListener(event -> 
		{
			listener.buttonClicked(Action.LOGIN_AS_ANOTHER);
			authnProcessor.logout(true);
		});
		
		buttonsVL.addComponents(buttonsHL, reloginB);
		buttonsVL.setComponentAlignment(buttonsHL, Alignment.BOTTOM_RIGHT);
		buttonsVL.setComponentAlignment(reloginB, Alignment.BOTTOM_RIGHT);
		setCompositionRoot(buttonsVL);
	}

	private void onAccept(ClickEvent event)
	{
		confirmB.removeClickShortcut();
		listener.buttonClicked(Action.ACCEPT);
	}

	public static interface ActionListener
	{
		void buttonClicked(Action accept);
	}
}
