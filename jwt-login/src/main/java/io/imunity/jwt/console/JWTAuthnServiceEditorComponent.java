/*
 * Copyright (c) 2024 Bixbit s.c. All rights reserved.
 *  See LICENSE file for licensing information.
 */
package io.imunity.jwt.console;

import java.util.List;

import com.vaadin.data.Binder;

import io.imunity.jwt.AuthzLoginTokenEndpoint;
import io.imunity.jwt.JWTAuthzWebEndpointFactory;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase;
import pl.edu.icm.unity.webui.console.services.tabs.AuthenticationTab;

class JWTAuthnServiceEditorComponent extends ServiceEditorBase
{
	private static final String TOKEN_SERVICE_NAME_SUFFIX = "_TOKEN";
	
	private Binder<JWTAuthnServiceConfiguration> jwtBinder;
	private Binder<DefaultServiceDefinition> serviceBinder;

	JWTAuthnServiceEditorComponent(MessageSource msg,
			JWTAuthnServiceEditorGeneralTab generalTab,
			AuthenticationTab authTab,
			DefaultServiceDefinition toEdit)
	{
		super(msg);
		boolean editMode = toEdit != null;

		jwtBinder = new Binder<>(JWTAuthnServiceConfiguration.class);
		serviceBinder = new Binder<>(DefaultServiceDefinition.class);

		generalTab.initUI(serviceBinder, jwtBinder, editMode);
		registerTab(generalTab);
		authTab.initUI(serviceBinder);
		registerTab(authTab);
		serviceBinder.setBean(editMode ? toEdit : new DefaultServiceDefinition(JWTAuthzWebEndpointFactory.TYPE.getName()));
		JWTAuthnServiceConfiguration config = new JWTAuthnServiceConfiguration();
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

		DefaultServiceDefinition webAuthz = serviceBinder.getBean();
		webAuthz.setConfiguration(jwtBinder.getBean().toProperties());
		
		DefaultServiceDefinition token = new DefaultServiceDefinition(AuthzLoginTokenEndpoint.TYPE.getName());
		token.setBinding(AuthzLoginTokenEndpoint.TYPE.getSupportedBinding());
		token.setAddress(webAuthz.getAddress() + "-token");
		token.setAuthenticationOptions(List.of("pwdComposite"));
		token.setName(webAuthz.getName() + TOKEN_SERVICE_NAME_SUFFIX);
		token.setRealm(webAuthz.getRealm());
		token.setConfiguration(webAuthz.getConfiguration());
		
		JWTAuthnServiceDefinition definition = new JWTAuthnServiceDefinition(webAuthz, token);
		
		return definition;
	}
}
