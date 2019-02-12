/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.api.utils;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.RegistrationForm;

/**
 * Generates and validates registration and enquiry forms
 * @author P.Piernik
 *
 */
public interface GroupDelegationConfigGenerator
{
	RegistrationForm generateRegistrationForm(String groupPath, String logo, List<String> attributes) throws EngineException;

	EnquiryForm generateJoinEnquiryForm(String groupPath, String logo) throws EngineException;

	EnquiryForm generateUpdateEnquiryForm(String groupPath, String logo) throws EngineException;
	
	List<String> validateRegistrationForm(String groupPath, String formName);
	
	List<String> validateJoinEnquiryForm(String groupPath, String formName);

	List<String> validateUpdateEnquiryForm(String groupPath, String formName);

	
}
