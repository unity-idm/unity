/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import java.util.List;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.ExternalResource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.column.RegistrationInfoProvider.RegistrationFormInfo;

/**
 * Top header component displayed in the AuthN screen.
 * 
 * Displays locale selector as well as registration form links if configured.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
class TopHeaderComponent extends CustomComponent
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, TopHeaderComponent.class);
	private UnityMessageSource msg;
	
	public TopHeaderComponent(Component localeChoice, boolean enableRegistration, 
			VaadinEndpointProperties config, RegistrationInfoProvider registrationInfoProvider, 
			UnityMessageSource msg)
	{
		this.msg = msg;
		init(localeChoice, enableRegistration, config.getRegistrationConfiguration(), registrationInfoProvider);
	}
	
	private void init(Component localeChoice, boolean enableRegistration, 
			EndpointRegistrationConfiguration endpointRegistrationConfiguration, 
			RegistrationInfoProvider registrationInfoProvider)
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
			try
			{
				List<RegistrationFormInfo> regInfo = registrationInfoProvider
						.getRegistrationFormLinkInfo(endpointRegistrationConfiguration.getEnabledForms());
				if (regInfo.size() == 1)
				{
					Component registrationLinksComponent = getRegistrationLinksComponent(regInfo.get(0));
					header.addComponent(registrationLinksComponent);
					header.setComponentAlignment(registrationLinksComponent, Alignment.MIDDLE_RIGHT);
				} else
				{
					if (regInfo.isEmpty() && endpointRegistrationConfiguration.getEnabledForms().isEmpty())
						LOG.warn("There are no public forms in the system allowed for registration. Signup link won't be added.");
					else if (regInfo.isEmpty() && !endpointRegistrationConfiguration.getEnabledForms().isEmpty())
						LOG.warn("There are no public forms in the system matching the "
								+ "ones configured on the endpoint: {}. Signup link won't be addded.", 
								endpointRegistrationConfiguration.getEnabledForms().toString());
					else if (regInfo.size() > 1 && endpointRegistrationConfiguration.getEnabledForms().isEmpty())
						LOG.warn("There are multiple public forms available in the system. "
								+ "Until you select one, the signup link won't be addded.");
					else if (regInfo.size() > 1 && !endpointRegistrationConfiguration.getEnabledForms().isEmpty())
						LOG.warn("There are multiple public forms configured for the endpoint. "
								+ "Until you select one, the signup link won't be addded.");
				}
			} catch (EngineException e)
			{
				LOG.error("Unable to generate component with registratino links, "
						+ "failed to retrieve registration forms.", e);
			}
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

	private Component getRegistrationLinksComponent(RegistrationFormInfo registrationInfo)
	{
		Link ret = new Link(msg.getMessage("AuthenticationUI.gotoSignUp"), new ExternalResource(registrationInfo.link));
		ret.addStyleName("u-authn-gotoSingup");
		return ret;
	}
}
