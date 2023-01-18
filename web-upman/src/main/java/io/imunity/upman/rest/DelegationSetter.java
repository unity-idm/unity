/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.RegistrationForm;

import java.util.List;

class DelegationSetter
{
	private final RestRegistrationForm registrationForm;
	private final RestSignUpEnquiry signUpEnquiry;
	private final RestMembershipEnquiry membershipUpdateEnquiry;
	private final String fullGroupName;
	private final String logoUrl;
	private final boolean enableSubprojects;
	private final List<String> readOnlyAttributes;

	private final GroupDelegationConfigGenerator groupDelegationConfigGenerator;
	private final RegistrationsManagement registrationsManagement;
	private final EnquiryManagement enquiryManagement;

	DelegationSetter(RestRegistrationForm registrationForm, RestSignUpEnquiry signUpEnquiry,
	                 RestMembershipEnquiry membershipUpdateEnquiry, String fullGroupName,
	                 String logoUrl, boolean enableSubprojects, List<String> readOnlyAttributes,
	                 GroupDelegationConfigGenerator groupDelegationConfigGenerator,
	                 RegistrationsManagement registrationsManagement, EnquiryManagement enquiryManagement)
	{
		this.registrationForm = registrationForm;
		this.signUpEnquiry = signUpEnquiry;
		this.membershipUpdateEnquiry = membershipUpdateEnquiry;
		this.fullGroupName = fullGroupName;
		this.logoUrl = logoUrl;
		this.enableSubprojects = enableSubprojects;
		this.readOnlyAttributes = readOnlyAttributes;
		this.groupDelegationConfigGenerator = groupDelegationConfigGenerator;
		this.registrationsManagement = registrationsManagement;
		this.enquiryManagement = enquiryManagement;
	}

	void setFor(Group group) throws EngineException
	{
		String registrationFormName = registrationForm.name;
		String joinEnquiryName = signUpEnquiry.name;
		String updateEnquiryName = membershipUpdateEnquiry.name;

		if (registrationForm.autogenerate)
		{
			RegistrationForm regForm = groupDelegationConfigGenerator
				.generateProjectRegistrationForm(
					fullGroupName, logoUrl, readOnlyAttributes);
			registrationsManagement.addForm(regForm);
			registrationFormName = regForm.getName();
		}
		if (signUpEnquiry.autogenerate)
		{
			EnquiryForm joinEnquiryForm = groupDelegationConfigGenerator
				.generateProjectJoinEnquiryForm(
					fullGroupName,
					logoUrl);
			enquiryManagement.addEnquiry(joinEnquiryForm);
			joinEnquiryName = joinEnquiryForm.getName();
		}
		if (membershipUpdateEnquiry.autogenerate)
		{
			EnquiryForm updateEnquiryForm = groupDelegationConfigGenerator
				.generateProjectUpdateEnquiryForm(
					fullGroupName,
					logoUrl);
			enquiryManagement.addEnquiry(updateEnquiryForm);
			updateEnquiryName = updateEnquiryForm.getName();
		}


		group.setDelegationConfiguration(new GroupDelegationConfiguration(true,
			enableSubprojects,
			logoUrl,
			registrationFormName, joinEnquiryName, updateEnquiryName,
			readOnlyAttributes)
		);
	}
}
