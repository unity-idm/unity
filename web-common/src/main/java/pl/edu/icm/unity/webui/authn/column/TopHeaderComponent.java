/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Top header component displayed in the AuthN screen.
 * 
 * Displays locale selector as well as registration form links if configured.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
class TopHeaderComponent extends CustomComponent
{
	private UnityMessageSource msg;
	
	public TopHeaderComponent(Component localeChoice, boolean enableRegistration, 
			VaadinEndpointProperties config, Runnable registrationLayoutLauncher, 
			UnityMessageSource msg)
	{
		this.msg = msg;
		init(localeChoice, enableRegistration, config.getRegistrationConfiguration(), registrationLayoutLauncher);
	}
	
	private void init(Component localeChoice, boolean enableRegistration, 
			EndpointRegistrationConfiguration endpointRegistrationConfiguration, 
			Runnable registrationLayoutLauncher)
	{
		VerticalLayout header = new VerticalLayout();
		header.setMargin(new MarginInfo(true, false, false, false));
		header.setSpacing(true);
		header.setWidth(100, Unit.PERCENTAGE);
		
		Component localeSelector = encapsulateLocaleChoice(localeChoice);
		header.addComponent(localeSelector);
		header.setExpandRatio(localeSelector, 1.0f);
		
		if (enableRegistration && endpointRegistrationConfiguration.isDisplayRegistrationFormsInHeader())
		{
			Button registrationButton = new Button(msg.getMessage("AuthenticationUI.gotoSignUp"));
			registrationButton.setStyleName(Styles.vButtonLink.toString());
			registrationButton.addStyleName("u-authn-gotoSingup");
			registrationButton.addClickListener(event -> registrationLayoutLauncher.run());
			header.addComponent(registrationButton);
			header.setComponentAlignment(registrationButton, Alignment.MIDDLE_RIGHT);
		}
		setCompositionRoot(header);
	}

	private Component encapsulateLocaleChoice(Component localeChoice)
	{
		VerticalLayout localeSelector = new VerticalLayout();
		localeSelector.setMargin(false);
		localeSelector.setSpacing(false);
		localeSelector.addComponent(localeChoice);
		localeSelector.setComponentAlignment(localeChoice, Alignment.MIDDLE_RIGHT);
		return localeSelector;
	}
}
