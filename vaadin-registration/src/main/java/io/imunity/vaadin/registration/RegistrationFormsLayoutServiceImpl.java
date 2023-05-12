/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.registration;

import com.google.common.collect.Lists;
import com.vaadin.flow.component.Component;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.RegistrationFormsLayoutService;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;

import java.util.List;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class RegistrationFormsLayoutServiceImpl implements RegistrationFormsLayoutService
{
	private final RegistrationsManagement registrationsManagement;
	private final MessageSource msg;
	private final UnityServerConfiguration cfg;
	private final IdPLoginController idpLoginController;
	private final RequestEditorCreator editorCreator;
	private final AutoLoginAfterSignUpProcessor autoLoginProcessor;
	private final NotificationPresenter notificationPresenter;
	private final VaadinLogoImageLoader logoImageLoader;
	private EndpointRegistrationConfiguration registrationConfiguration;


	@Autowired
	RegistrationFormsLayoutServiceImpl(MessageSource msg,
	                                   @Qualifier("insecure") RegistrationsManagement regMan,
	                                   UnityServerConfiguration cfg,
	                                   IdPLoginController idpLoginController,
	                                   RequestEditorCreator editorCreator,
	                                   AutoLoginAfterSignUpProcessor autoLogin, VaadinLogoImageLoader logoImageLoader,
	                                   NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.registrationsManagement = regMan;
		this.cfg = cfg;
		this.idpLoginController = idpLoginController;
		this.editorCreator = editorCreator;
		this.autoLoginProcessor = autoLogin;
		this.logoImageLoader = logoImageLoader;
		this.notificationPresenter = notificationPresenter;
	}

	public void configure(EndpointRegistrationConfiguration registrationConfiguration)
	{
		this.registrationConfiguration = registrationConfiguration;
	}

	public boolean isRegistrationEnabled() throws EngineException
	{
		if (!registrationConfiguration.isShowRegistrationOption())
			return false;

		List<RegistrationForm> displayedForms = getDisplayedForms();

		return !displayedForms.isEmpty();
	}

	public List<RegistrationForm> getDisplayedForms() throws EngineException
	{
		List<RegistrationForm> displayedForms = Lists.newArrayList();
		List<String> allowedForms = registrationConfiguration.getEnabledForms();
		List<RegistrationForm> forms = registrationsManagement.getForms();
		for (RegistrationForm form : forms)
		{
			if (!form.isPubliclyAvailable())
				continue;
			if (allowedForms != null && !allowedForms.isEmpty() && !allowedForms.contains(form.getName()))
				continue;
			displayedForms.add(form);
		}
		return displayedForms;
	}

	public Component createRegistrationView(RegistrationForm form, TriggeringMode mode, Runnable customCancelHandler,
	                                        Runnable completedRegistrationHandler, Runnable gotoSignInRedirector)
	{
		StandaloneRegistrationView standaloneRegistrationView = new StandaloneRegistrationView(
				msg, registrationsManagement, cfg, idpLoginController, editorCreator, autoLoginProcessor,
				logoImageLoader, notificationPresenter
		);
		standaloneRegistrationView.init(form, mode, customCancelHandler, completedRegistrationHandler, gotoSignInRedirector);
		return standaloneRegistrationView;
	}
}
