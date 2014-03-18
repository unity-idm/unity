/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.registration;

import org.springframework.stereotype.Component;

/**
 * Definition of the template of the updated registration request message.
 * @author P. Piernik
 */
@Component
public class UpdateRegistrationTemplateDef extends RegistrationWithCommentsTemplateDef
{
	public static final String NAME = "RegistrationRequestUpdated";
	
	public UpdateRegistrationTemplateDef()
	{
		super(NAME, "MessageTemplateConsumer.UpdateForm.desc");
	}
}
