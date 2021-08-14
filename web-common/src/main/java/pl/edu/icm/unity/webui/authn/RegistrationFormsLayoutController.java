/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.List;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.forms.reg.StandaloneRegistrationView;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class RegistrationFormsLayoutController
{
	private EndpointRegistrationConfiguration registrationConfiguration;
	private RegistrationsManagement registrationsManagement;
	private ObjectFactory<StandaloneRegistrationView> viewFactory;

	@Autowired
	public RegistrationFormsLayoutController(
			@Qualifier("insecure") RegistrationsManagement registrationsManagement,
			ObjectFactory<StandaloneRegistrationView> viewFactory)
	{
		this.registrationsManagement = registrationsManagement;
		this.viewFactory = viewFactory;
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

	public StandaloneRegistrationView createRegistrationView(RegistrationForm form)
	{
		return viewFactory.getObject().init(form);
	}
}
