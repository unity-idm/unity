/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.registration;

import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.UserRequestState;

/**
 * Various internally useful operations, related to forms automation. 
 * The implementation relies on the profile. It is therefore stateful, tied to a particular 
 * form.
 * @author K. Benedyczak
 */
public interface FormAutomationSupport
{
	AutomaticRequestAction getAutoProcessAction(UserRequestState<? extends BaseRegistrationInput> request,
			RequestSubmitStatus status);
}