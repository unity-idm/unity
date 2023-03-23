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
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.authn.WebLogoutHandler;

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

	public IdPButtonsBar(MessageSource msg, WebLogoutHandler authnProcessor, String logoutRedirectPath,
	                     ActionListener listener)
	{
		this.msg = msg;
		this.authnProcessor = authnProcessor;
		this.listener = listener;
		this.logoutRedirectPath = logoutRedirectPath;
		initUI();
	}

	private void initUI()
	{
		setSizeUndefined();
		setMargin(false);
		setPadding(false);

		Button confirmButton = new Button(msg.getMessage("IdPButtonsBar.confirm"));
		confirmButton.setId("IdpButtonsBar.confirmButton");
		confirmButton.addClickListener(e -> {
			listener.buttonClicked(Action.ACCEPT);
		});
		confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		confirmButton.addClassName("u-consentConfirmButton");
		confirmButton.addClickShortcut(Key.ENTER);
		
		Button declineButton = new Button(msg.getMessage("IdPButtonsBar.decline"));
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

	public interface ActionListener
	{
		void buttonClicked(Action accept);
	}
}
