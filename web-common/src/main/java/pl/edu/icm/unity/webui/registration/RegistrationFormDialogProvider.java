/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.registration;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.common.AbstractDialog;

/**
 * Provides registration form dialog. Implemented by {@link RegistrationFormLauncher} 
 * and {@link InsecureRegistrationFormLauncher}
 * @author K. Benedyczak
 */
public interface RegistrationFormDialogProvider
{
	AbstractDialog getDialog(final RegistrationForm form, 
			RemotelyAuthenticatedContext remoteContext, TriggeringMode mode) throws EngineException;

}
