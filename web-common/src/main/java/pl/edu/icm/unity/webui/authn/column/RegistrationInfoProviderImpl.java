/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.RegistrationForm;

/**
 * Provides information about configured and available registration forms that
 * are shown on the authN page.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class RegistrationInfoProviderImpl implements RegistrationInfoProvider
{
	private final RegistrationsManagement registrationsManagement;
	private SharedEndpointManagement sharedEndpointMan;
	private UnityMessageSource msg;

	@Autowired
	public RegistrationInfoProviderImpl(@Qualifier("insecure") RegistrationsManagement registrationsManagement,
			SharedEndpointManagement sharedEndpointMan, UnityMessageSource msg)
	{
		this.registrationsManagement = registrationsManagement;
		this.sharedEndpointMan = sharedEndpointMan;
		this.msg = msg;
	}

	@Override
	public List<RegistrationFormInfo> getRegistrationFormLinksInfo(Collection<String> configuredForms) throws EngineException
	{
		List<RegistrationFormInfo> infos = Lists.newArrayList();
		Map<String, RegistrationForm> formsByName = registrationsManagement.getForms().stream()
				.collect(Collectors.toMap(RegistrationForm::getName, Function.identity()));
		for (String configuredForm : configuredForms)
		{
			RegistrationForm form = formsByName.get(configuredForm);
			if (form != null)
			{
				String displayedName = form.getDisplayedName().getValue(msg);
				String link = PublicRegistrationURLSupport.getPublicRegistrationLink(form, sharedEndpointMan);
				infos.add(new RegistrationFormInfo(displayedName, link));
			}
		}
		return infos;
	}
}
