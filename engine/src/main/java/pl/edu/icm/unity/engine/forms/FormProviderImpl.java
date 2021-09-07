/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.IllegalFormTypeException;
import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.FormType;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.invite.FormProvider;

@Component
class FormProviderImpl implements FormProvider
{
	private final RegistrationFormDB registrationDB;
	private final EnquiryFormDB enquiryDB;

	@Autowired
	FormProviderImpl(RegistrationFormDB registrationDB, EnquiryFormDB enquiryDB)
	{
		this.registrationDB = registrationDB;
		this.enquiryDB = enquiryDB;
	}

	@Override
	public RegistrationForm getRegistrationForm(String formId)
	{
		return registrationDB.get(formId);
	}

	@Override
	public EnquiryForm getEnquiryForm(String formId)
	{
		return enquiryDB.get(formId);
	}
	
	@Override
	public BaseForm getForm(String formId, FormType formType) throws IllegalFormTypeException
	{
		switch (formType)
		{
		case REGISTRATION:
			return getRegistrationForm(formId);
		case ENQUIRY:
			return getEnquiryForm(formId);
		default:
			throw new IllegalFormTypeException("Invalid form type");
		}
	}

}
