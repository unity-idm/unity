/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.api.utils;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.RegistrationForm;

/**
 * Generates and validates registration and enquiry forms
 * @author P.Piernik
 *
 */
public interface GroupDelegationConfigGenerator
{
	RegistrationForm generateRegistrationForm(Group g, String logo, List<String> attributes) throws EngineException;

	EnquiryForm generateJoinEnquiryForm(Group group, String logo) throws EngineException;

	List<String> validateRegistrationForm(String formName, String groupPath);
	
	List<String> validateJoinEnquiryForm(String formName, String groupPath);
}
