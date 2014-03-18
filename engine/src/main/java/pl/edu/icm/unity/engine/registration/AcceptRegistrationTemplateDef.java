/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.registration;

import org.springframework.stereotype.Component;

/**
 * Definition of the template of the accepted registration request message.
 * @author P. Piernik
 */
@Component
public class AcceptRegistrationTemplateDef extends RegistrationWithCommentsTemplateDef
{
	public static final String NAME = "RegistrationRequestAccepted";
	
	public AcceptRegistrationTemplateDef()
	{
		super(NAME, "MessageTemplateConsumer.AcceptForm.desc");
	}
}
