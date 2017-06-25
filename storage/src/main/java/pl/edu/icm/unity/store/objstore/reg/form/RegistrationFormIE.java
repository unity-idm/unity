/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg.form;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;
import pl.edu.icm.unity.types.registration.RegistrationForm;

/**
 * Handles import/export of {@link RegistrationForm}.
 * @author K. Benedyczak
 */
@Component
public class RegistrationFormIE extends GenericObjectIEBase<RegistrationForm>
{
	@Autowired
	public RegistrationFormIE(RegistrationFormDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, RegistrationForm.class, 112, 
				RegistrationFormHandler.REGISTRATION_FORM_OBJECT_TYPE);
	}
}



