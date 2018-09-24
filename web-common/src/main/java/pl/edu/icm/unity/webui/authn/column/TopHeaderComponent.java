/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import java.util.List;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.ExternalResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
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
	
	public TopHeaderComponent(Component localeChoice, boolean enableRegistration, 
			VaadinEndpointProperties config, RegistrationInfoProvider registrationInfoProvider)
	{
		init(localeChoice, enableRegistration, config.getRegistrationConfiguration(), registrationInfoProvider);
	}
	
	private void init(Component localeChoice, boolean enableRegistration, 
			EndpointRegistrationConfiguration endpointRegistrationConfiguration, 
			RegistrationInfoProvider registrationInfoProvider)
	{
		HorizontalLayout header = new HorizontalLayout();
		header.setMargin(false);
		header.setSpacing(false);
		header.setWidth(100, Unit.PERCENTAGE);
		
		Component localeSelector = encapsulateLocaleChoice(localeChoice);
		header.addComponent(localeSelector);
		header.setExpandRatio(localeSelector, 1.0f);
		
		if (enableRegistration 
				&& endpointRegistrationConfiguration.isDisplayRegistrationFormsInHeader()
				&& !endpointRegistrationConfiguration.getEnabledForms().isEmpty())
		{
			try
			{
				List<RegistrationFormInfo> infos = registrationInfoProvider
						.getRegistrationFormLinksInfo(endpointRegistrationConfiguration.getEnabledForms());
				Component registrationLinksComponent = getRegistrationLinksComponent(infos);
				header.addComponent(registrationLinksComponent);
				header.setComponentAlignment(registrationLinksComponent, Alignment.MIDDLE_RIGHT);
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
		localeSelector.setMargin(true);
		localeSelector.setSpacing(false);
		localeSelector.addComponent(localeChoice);
		localeSelector.setComponentAlignment(localeChoice, Alignment.MIDDLE_RIGHT);
		return localeSelector;
	}

	private Component getRegistrationLinksComponent(List<RegistrationFormInfo> registrationInfos)
	{
		VerticalLayout links = new VerticalLayout();
		links.setMargin(true);
		links.setSpacing(false);
		links.setWidthUndefined();
		
		for (RegistrationFormInfo info  : registrationInfos)
		{
			Link returnUrlLink =  new Link(info.displayedName, new ExternalResource(info.link));
			links.addComponent(returnUrlLink);
		}
		
		return links;
	}
}
