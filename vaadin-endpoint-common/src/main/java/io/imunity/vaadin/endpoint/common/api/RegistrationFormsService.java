/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api;

import com.vaadin.flow.component.Component;
import io.imunity.vaadin.endpoint.common.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.registration.RegistrationContext;
import pl.edu.icm.unity.base.registration.RegistrationForm;

import java.util.List;

public interface RegistrationFormsService
{
	void configure(EndpointRegistrationConfiguration registrationConfiguration);
	boolean isRegistrationEnabled() throws EngineException;
	List<RegistrationForm> getDisplayedForms() throws EngineException;
	Component createRegistrationView(RegistrationForm form, RegistrationContext.TriggeringMode mode, Runnable customCancelHandler, Runnable completedRegistrationHandler,
	                                 Runnable gotoSignInRedirector);
}
