/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine.registration;

import org.springframework.stereotype.Component;

/**
 * Definition of a template of a rejected registration request message.
 * @author P. Piernik
 */
@Component
public class RejectRegistrationTemplateDef extends RegistrationWithCommentsTemplateDef
{
	public static final String NAME = "RegistrationRequestRejected";
	
	public RejectRegistrationTemplateDef()
	{
		super(NAME, "MessageTemplateConsumer.RejectForm.desc");
	}
}
