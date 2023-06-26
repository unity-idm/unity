/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.LinkButton;
import io.imunity.vaadin.endpoint.common.LocaleChoiceComponent;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;

import java.util.Optional;

/**
 * Top header component displayed in the AuthN screen.
 * 
 * Displays locale selector as well as registration form links if configured.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
class TopHeaderComponent extends VerticalLayout
{
	private final MessageSource msg;
	
	public TopHeaderComponent(Optional<LocaleChoiceComponent> localeChoice, boolean enableRegistration,
	                          VaadinEndpointProperties config, Runnable registrationLayoutLauncher,
	                          MessageSource msg)
	{
		this.msg = msg;
		init(localeChoice, enableRegistration, config.getRegistrationConfiguration(), registrationLayoutLauncher);
	}
	
	private void init(Optional<LocaleChoiceComponent> localeChoice, boolean enableRegistration, 
			EndpointRegistrationConfiguration endpointRegistrationConfiguration,
			Runnable registrationLayoutLauncher)
	{
		setAlignItems(Alignment.END);
		setWidthFull();

		localeChoice.ifPresent(this::add);
		if (enableRegistration && endpointRegistrationConfiguration.isDisplayRegistrationFormsInHeader())
		{
			Div gotoSignIn = new LinkButton(msg.getMessage("AuthenticationUI.gotoSignUp"), e -> registrationLayoutLauncher.run());
			gotoSignIn.addClassName("u-reg-gotoSignIn");
			add(gotoSignIn);
		}
	}
}
