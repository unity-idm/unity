/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.reg.form;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;
import pl.edu.icm.unity.types.registration.RegistrationForm;

/**
 * Handler for {@link RegistrationForm}s storage.
 * @author K. Benedyczak
 */
@Component
public class RegistrationFormHandler extends DefaultEntityHandler<RegistrationForm>
{
	public static final String REGISTRATION_FORM_OBJECT_TYPE = "registrationForm";
	
	@Autowired
	public RegistrationFormHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, REGISTRATION_FORM_OBJECT_TYPE, RegistrationForm.class);
	}

	@Override
	public GenericObjectBean toBlob(RegistrationForm value)
	{
		return new GenericObjectBean(value.getName(), 
				JsonUtil.serialize2Bytes(value.toJson()), supportedType);
	}

	@Override
	public RegistrationForm fromBlob(GenericObjectBean blob)
	{
		return new RegistrationForm(JsonUtil.parse(blob.getContents()));
	}
}
