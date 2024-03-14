/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.consent_utils;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.LinkButton;
import io.imunity.vaadin.endpoint.common.WebLogoutHandler;
import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Bar with buttons: Accept, Decline, Login as another user. The login as another user action is 
 * typically handled internally and needs no additional intervention. 
 * @author K. Benedyczak
 */
public class IdPButtonsBar extends VerticalLayout
{
	public enum Action {ACCEPT, DENY, LOGIN_AS_ANOTHER}

	private final MessageSource msg;
	private final WebLogoutHandler authnProcessor;
	private final ActionListener listener;
	private final String logoutRedirectPath;
	private final Button confirmButton;
	private final Button declineButton;

	public IdPButtonsBar(MessageSource msg, WebLogoutHandler authnProcessor, String logoutRedirectPath,
	                     ActionListener listener)
	{
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.listener = listener;
		this.logoutRedirectPath = logoutRedirectPath;
		this.confirmButton = new Button(msg.getMessage("IdPButtonsBar.confirm"));
		this.declineButton = new Button(msg.getMessage("IdPButtonsBar.decline"));

		initUI();
	}

	private void initUI()
	{
		setSizeUndefined();
		setMargin(false);
		setPadding(false);

		confirmButton.setId("IdpButtonsBar.confirmButton");
		confirmButton.addClickListener(e -> {
			listener.buttonClicked(Action.ACCEPT);
		});
		confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		confirmButton.addClassName("u-consentConfirmButton");
		confirmButton.addClickShortcut(Key.ENTER);
		
		declineButton.addClickListener(event -> listener.buttonClicked(Action.DENY));
		declineButton.addClassName("u-consentDeclineButton");

		HorizontalLayout buttonsLayout = new HorizontalLayout();
		buttonsLayout.setMargin(false);
		buttonsLayout.setPadding(false);
		buttonsLayout.add(declineButton, confirmButton);

		LinkButton reloginButton = new LinkButton(msg.getMessage("IdPButtonsBar.logAsAnother"), event ->
		{
			listener.buttonClicked(Action.LOGIN_AS_ANOTHER);
			authnProcessor.logout(true, logoutRedirectPath);
		});

		add(buttonsLayout, reloginButton);
	}

	public void setConfirmButtonText(String txt)
	{
		confirmButton.setText(txt);
	}

	public void setDeclineButtonText(String txt)
	{
		declineButton.setText(txt);
	}

	public interface ActionListener
	{
		void buttonClicked(Action accept);
	}
}
