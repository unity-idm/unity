/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.registration;

import org.springframework.stereotype.Component;


/**
 * Definition of the template of the submitted registration request message.
 * @author P. Piernik
 */
@Component
public class SubmitRegistrationTemplateDef extends BaseRegistrationTemplateDef
{
	public static final String NAME = "RegistrationRequestSubmitted";
	
	public SubmitRegistrationTemplateDef()
	{
		super(NAME, "MessageTemplateConsumer.SubmitForm.desc");
	}
}
