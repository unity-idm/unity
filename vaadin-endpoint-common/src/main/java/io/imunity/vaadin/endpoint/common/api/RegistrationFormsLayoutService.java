/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api;

import com.vaadin.flow.component.Component;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;

import java.util.List;

public interface RegistrationFormsLayoutService
{
	void configure(EndpointRegistrationConfiguration registrationConfiguration);
	boolean isRegistrationEnabled() throws EngineException;
	List<RegistrationForm> getDisplayedForms() throws EngineException;
	Component createRegistrationView(RegistrationForm form, RegistrationContext.TriggeringMode mode, Runnable customCancelHandler, Runnable completedRegistrationHandler,
	                                 Runnable gotoSignInRedirector);
}
