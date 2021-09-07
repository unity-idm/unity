/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.registration.invite;

import pl.edu.icm.unity.exceptions.IllegalFormTypeException;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.FormType;
import pl.edu.icm.unity.types.registration.RegistrationForm;

public interface FormProvider
{
	RegistrationForm getRegistrationForm(String formId);
	EnquiryForm getEnquiryForm(String formId);
	BaseForm getForm(String formId, FormType formType) throws IllegalFormTypeException;
}
