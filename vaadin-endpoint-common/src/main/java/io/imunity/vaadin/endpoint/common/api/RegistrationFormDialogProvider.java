/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.api;

import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;

public interface RegistrationFormDialogProvider
{
	void showRegistrationDialog(
			RegistrationForm form,
			RemotelyAuthenticatedPrincipal remoteContext, TriggeringMode mode,
			AsyncErrorHandler errorHandler);

	void showRegistrationDialog(
			String form,
			RemotelyAuthenticatedPrincipal remoteContext, TriggeringMode mode,
			AsyncErrorHandler errorHandler) throws EngineException;

	interface AsyncErrorHandler
	{
		void onError(Exception e);
	}
}
