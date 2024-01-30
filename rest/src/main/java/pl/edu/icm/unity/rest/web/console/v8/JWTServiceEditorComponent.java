/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.web.console.v8;

import com.vaadin.data.Binder;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.rest.jwt.endpoint.JWTManagementEndpoint;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase;
import pl.edu.icm.unity.webui.console.services.tabs.AuthenticationTab;

/**
 * JWT service editor component
 * 
 * @author P.Piernik
 *
 */
class JWTServiceEditorComponent extends ServiceEditorBase
{
	private Binder<JWTServiceConfiguration> jwtBinder;
	private Binder<DefaultServiceDefinition> serviceBinder;

	JWTServiceEditorComponent(MessageSource msg, JWTServiceEditorGeneralTab generalTab, AuthenticationTab authTab, DefaultServiceDefinition toEdit)
	{
		super(msg);
		boolean editMode = toEdit != null;

		jwtBinder = new Binder<>(JWTServiceConfiguration.class);
		serviceBinder = new Binder<>(DefaultServiceDefinition.class);
		
		generalTab.initUI(serviceBinder, jwtBinder, editMode);
		registerTab(generalTab);
		authTab.initUI(serviceBinder);
		registerTab(authTab);
		serviceBinder.setBean(
				editMode ? toEdit : new DefaultServiceDefinition(JWTManagementEndpoint.TYPE.getName()));
		JWTServiceConfiguration config = new JWTServiceConfiguration();
		if (editMode && toEdit.getConfiguration() != null)
		{
			config.fromProperties(toEdit.getConfiguration(), msg);
		}
		jwtBinder.setBean(config);
	}

	public ServiceDefinition getServiceDefiniton() throws FormValidationException
	{
		boolean hasErrors = serviceBinder.validate().hasErrors();
		hasErrors |= jwtBinder.validate().hasErrors();
		if (hasErrors)
		{
			setErrorInTabs();
			throw new FormValidationException();
		}

		DefaultServiceDefinition service = serviceBinder.getBean();
		service.setConfiguration(jwtBinder.getBean().toProperties());
		return service;
	}
}
