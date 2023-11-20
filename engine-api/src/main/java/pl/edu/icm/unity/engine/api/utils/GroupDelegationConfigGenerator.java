/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.api.utils;

import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.FormType;
import pl.edu.icm.unity.types.registration.RegistrationForm;

/**
 * Generates and validates registration and enquiry forms
 * @author P.Piernik
 *
 */
public interface GroupDelegationConfigGenerator
{
	RegistrationForm generateProjectRegistrationForm(String groupPath, String logo, List<String> attributes, List<Long> policyDocuments) throws EngineException;

	EnquiryForm generateProjectJoinEnquiryForm(String groupPath, String logo, List<Long> policyDocuments) throws EngineException;

	EnquiryForm generateProjectUpdateEnquiryForm(String groupPath, String logo) throws EngineException;
	
	List<String> validateRegistrationForm(String groupPath, String formName, Set<Long> projectPolicyDocumentsIds);
	
	List<String> validateJoinEnquiryForm(String groupPath, String formName, Set<Long> projectPolicyDocumentsIds);

	List<String> validateUpdateEnquiryForm(String groupPath, String formName);

	RegistrationForm generateSubprojectRegistrationForm(String toCopy, String projectPath,
			String subprojectPath, String logo);

	EnquiryForm generateSubprojectUpdateEnquiryForm(String toCopyName, String projectPath, String subprojectPath,
			String logo);

	EnquiryForm generateSubprojectJoinEnquiryForm(String toCopyName, String projectPath, String subprojectPath,
			String logo);

	void synchronizePolicy(String formName, FormType formType, List<Long> projectPolicyDocumentsIds) throws EngineException;
	
}
