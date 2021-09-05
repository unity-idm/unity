/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.registration;

import pl.edu.icm.unity.exceptions.IllegalFormTypeException;
import pl.edu.icm.unity.types.registration.FormType;
import pl.edu.icm.unity.types.registration.RegistrationForm;

/**
 * Defines constants and helper methods used to create public form access URI. 
 * Note that the public form filling code is in principle implemented in web endpoints,
 * however possibility to link to it is required in the core engine, for instance to fill 
 * invitation messages.
 * 
 * @author Krzysztof Benedyczak
 */
public interface PublicRegistrationURLSupport
{

	String REGISTRATION_VIEW = "registration";
	String ENQUIRY_VIEW = "enquiry";
	String CODE_PARAM = "regcode";
	String FORM_PARAM = "form";

	/**
	 * @param formName
	 * @param sharedEndpointMan
	 * @return a link to a standalone UI of a registration form
	 */
	String getPublicRegistrationLink(RegistrationForm form);

	/**
	 * @param formName
	 * @param sharedEndpointMan
	 * @return a link to a standalone UI of an enquiry form
	 */
	String getWellknownEnquiryLink(String formName);

	/**
	 * @param formName
	 * @param sharedEndpointMan
	 * @return a link to a standalone UI of a registration form with included registration code
	 */
	String getPublicRegistrationLink(String form, String code);

	/**
	 * @param formName
	 * @param sharedEndpointMan
	 * @return a link to a standalone UI of a enquiry form with included registration code
	 */
	String getPublicEnquiryLink(String form, String code);

	/**
	 * 
	 * @param form
	 * @param formType
	 * @param code
	 * @return a link to a standalone UI of a form with included registration code
	 * @throws IllegalFormTypeException
	 */
	String getPublicFormLink(String form, FormType formType, String code) throws IllegalFormTypeException;

}