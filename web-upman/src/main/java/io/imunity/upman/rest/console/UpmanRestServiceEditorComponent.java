/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest.console;

import com.vaadin.data.Binder;
import io.imunity.upman.rest.RESTUpmanEndpoint;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase;
import pl.edu.icm.unity.webui.console.services.tabs.AuthenticationTab;


class UpmanRestServiceEditorComponent extends ServiceEditorBase
{
	private final Binder<UpmanRestServiceConfiguration> restBinder;
	private final Binder<DefaultServiceDefinition> serviceBinder;

	public UpmanRestServiceEditorComponent(MessageSource msg, UpmanRestServiceEditorGeneralTab generalTab,
	                                       AuthenticationTab authTab, DefaultServiceDefinition toEdit)
	{
		super(msg);
		boolean editMode = toEdit != null;
		restBinder = new Binder<>(UpmanRestServiceConfiguration.class);
		serviceBinder = new Binder<>(DefaultServiceDefinition.class);
		
		generalTab.initUI(serviceBinder, restBinder, editMode);
		registerTab(generalTab);
		authTab.initUI(serviceBinder);
		registerTab(authTab);
		
		serviceBinder.setBean(
				editMode ? toEdit : new DefaultServiceDefinition(RESTUpmanEndpoint.TYPE.getName()));
		UpmanRestServiceConfiguration config = new UpmanRestServiceConfiguration();
		if (editMode && toEdit.getConfiguration() != null)
		{
			config.fromProperties(toEdit.getConfiguration(), msg);
		}
		restBinder.setBean(config);
	}

	ServiceDefinition getServiceDefiniton() throws FormValidationException
	{
		boolean hasErrors = serviceBinder.validate().hasErrors();
		hasErrors |= restBinder.validate().hasErrors();
		if (hasErrors)
		{
			setErrorInTabs();
			throw new FormValidationException();
		}

		DefaultServiceDefinition service = serviceBinder.getBean();
		service.setConfiguration(restBinder.getBean().toProperties());
		return service;
	}
}
