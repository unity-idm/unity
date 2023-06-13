/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.api.utils;

import java.util.List;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.RegistrationForm;

/**
 * Generates and validates registration and enquiry forms
 * @author P.Piernik
 *
 */
public interface GroupDelegationConfigGenerator
{
	RegistrationForm generateProjectRegistrationForm(String groupPath, String logo, List<String> attributes) throws EngineException;

	EnquiryForm generateProjectJoinEnquiryForm(String groupPath, String logo) throws EngineException;

	EnquiryForm generateProjectUpdateEnquiryForm(String groupPath, String logo) throws EngineException;
	
	List<String> validateRegistrationForm(String groupPath, String formName);
	
	List<String> validateJoinEnquiryForm(String groupPath, String formName);

	List<String> validateUpdateEnquiryForm(String groupPath, String formName);

	RegistrationForm generateSubprojectRegistrationForm(String toCopy, String projectPath,
			String subprojectPath, String logo);

	EnquiryForm generateSubprojectUpdateEnquiryForm(String toCopyName, String projectPath, String subprojectPath,
			String logo);

	EnquiryForm generateSubprojectJoinEnquiryForm(String toCopyName, String projectPath, String subprojectPath,
			String logo);

	
}
