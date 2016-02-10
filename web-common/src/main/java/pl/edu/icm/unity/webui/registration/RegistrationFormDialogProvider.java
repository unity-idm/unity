/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.registration;

import pl.edu.icm.unity.server.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.AsyncErrorHandler;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;

/**
 * Provides registration form dialog. Implemented by {@link AdminRegistrationFormLauncher} 
 * and {@link InsecureRegistrationFormLauncher}
 * @author K. Benedyczak
 */
public interface RegistrationFormDialogProvider
{
	void showRegistrationDialog(final RegistrationForm form, 
			RemotelyAuthenticatedContext remoteContext, TriggeringMode mode,
			AsyncErrorHandler errorHandler);
	
	/**
	 * @return registration code as provided in URL parameter or null if not present
	 */
	public static String getCodeFromURL()
	{
		VaadinRequest currentRequest = VaadinService.getCurrentRequest();
		if (currentRequest == null)
			return null;
		return currentRequest.getParameter(PublicRegistrationURLSupport.CODE_PARAM);
	}
}
