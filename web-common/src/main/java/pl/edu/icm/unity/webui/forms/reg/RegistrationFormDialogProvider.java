/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;

import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.webui.AsyncErrorHandler;

/**
 * Provides registration form dialog.
 * @author K. Benedyczak
 */
public interface RegistrationFormDialogProvider
{
	void showRegistrationDialog(final RegistrationForm form, 
			RemotelyAuthenticatedPrincipal remoteContext, TriggeringMode mode,
			AsyncErrorHandler errorHandler);
	
	/**
	 * @return registration code as provided in URL parameter or null if not present
	 */
	public static String getCodeFromURL()
	{
		return getParamOrNullFromURL(PublicRegistrationURLSupport.CODE_PARAM);
	}
	
	public static String getFormFromURL()
	{
		return getParamOrNullFromURL(PublicRegistrationURLSupport.FORM_PARAM);
	}
	
	public static String getParamOrNullFromURL(String param)
	{
		VaadinRequest currentRequest = VaadinService.getCurrentRequest();
		if (currentRequest == null)
			return null;
		return currentRequest.getParameter(param);
	}
}
