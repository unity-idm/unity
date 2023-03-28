/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg.form;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase2;
import pl.edu.icm.unity.types.registration.RegistrationForm;

/**
 * Handles import/export of {@link RegistrationForm}.
 * 
 * @author K. Benedyczak
 */
@Component
public class RegistrationFormIE extends GenericObjectIEBase2<RegistrationForm>
{
	@Autowired
	public RegistrationFormIE(RegistrationFormDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, 112, RegistrationFormHandler.REGISTRATION_FORM_OBJECT_TYPE);
	}

	@Override
	protected RegistrationForm convert(ObjectNode src)
	{
		return RegistrationFormMapper.map(jsonMapper.convertValue(src, DBRegistrationForm.class));
	}

	@Override
	protected ObjectNode convert(RegistrationForm src)
	{
		return jsonMapper.convertValue(RegistrationFormMapper.map(src), ObjectNode.class);
	}
}
