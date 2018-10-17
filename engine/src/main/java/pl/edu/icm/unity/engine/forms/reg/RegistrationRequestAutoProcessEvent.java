/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.reg;

import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;

/**
 * Sent to trigger auto-processing of a registration request
 * @author K. Benedyczak
 */
public class RegistrationRequestAutoProcessEvent
{
	public final RegistrationForm form;
	public final RegistrationRequestState requestFull;
	public final String logMessageTemplate;

	public RegistrationRequestAutoProcessEvent(RegistrationForm form, RegistrationRequestState requestFull,
			String logMessageTemplate)
	{
		this.form = form;
		this.requestFull = requestFull;
		this.logMessageTemplate = logMessageTemplate;
	}
}
