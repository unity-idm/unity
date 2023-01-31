/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.RegistrationForm;

import javax.ws.rs.BadRequestException;
import java.util.List;

class DelegationComputer
{
	private final String fullGroupName;
	private final String logoUrl;
	private final List<String> readOnlyAttributes;
	private final GroupDelegationConfigGenerator groupDelegationConfigGenerator;
	private final RegistrationsManagement registrationsManagement;
	private final EnquiryManagement enquiryManagement;

	private DelegationComputer(String fullGroupName, String logoUrl,
	                           List<String> readOnlyAttributes, GroupDelegationConfigGenerator groupDelegationConfigGenerator,
	                           RegistrationsManagement registrationsManagement, EnquiryManagement enquiryManagement)
	{
		this.fullGroupName = fullGroupName;
		this.logoUrl = logoUrl;
		this.readOnlyAttributes = readOnlyAttributes;
		this.groupDelegationConfigGenerator = groupDelegationConfigGenerator;
		this.registrationsManagement = registrationsManagement;
		this.enquiryManagement = enquiryManagement;
	}

	public String computeMembershipUpdateEnquiryName(RestMembershipEnquiry membershipUpdateEnquiry) throws EngineException
	{
		if(membershipUpdateEnquiry == null)
			return null;
		String updateEnquiryName = membershipUpdateEnquiry.name;
		if (membershipUpdateEnquiry.autogenerate)
		{
			EnquiryForm updateEnquiryForm = groupDelegationConfigGenerator
				.generateProjectUpdateEnquiryForm(
					fullGroupName,
					logoUrl);
			enquiryManagement.addEnquiry(updateEnquiryForm);
			updateEnquiryName = updateEnquiryForm.getName();
		} else if (membershipUpdateEnquiry.name != null)
		{
			if(!enquiryManagement.hasForm(membershipUpdateEnquiry.name))
				throw new BadRequestException("Form named " + membershipUpdateEnquiry.name + " does not exist");
		}
		return updateEnquiryName;
	}

	public String computeSignUpEnquiryName(RestSignUpEnquiry signUpEnquiry) throws EngineException
	{
		if(signUpEnquiry == null)
			return null;
		String joinEnquiryName = signUpEnquiry.name;
		if (signUpEnquiry.autogenerate)
		{
			EnquiryForm joinEnquiryForm = groupDelegationConfigGenerator
				.generateProjectJoinEnquiryForm(
					fullGroupName,
					logoUrl);
			enquiryManagement.addEnquiry(joinEnquiryForm);
			joinEnquiryName = joinEnquiryForm.getName();
		}
		else if (signUpEnquiry.name != null)
		{
			if(!enquiryManagement.hasForm(signUpEnquiry.name))
				throw new BadRequestException("Form named " + signUpEnquiry.name + " does not exist");
		}
		return joinEnquiryName;
	}

	public String computeRegistrationFormName(RestRegistrationForm registrationForm) throws EngineException
	{
		if(registrationForm == null)
			return null;
		String registrationFormName = registrationForm.name;
		if (registrationForm.autogenerate)
		{
			RegistrationForm regForm = groupDelegationConfigGenerator
				.generateProjectRegistrationForm(
					fullGroupName, logoUrl, readOnlyAttributes);
			registrationsManagement.addForm(regForm);
			registrationFormName = regForm.getName();
		}
		else if (registrationForm.name != null)
		{
			if(!registrationsManagement.hasForm(registrationForm.name))
				throw new BadRequestException("Form named " + registrationForm.name + " does not exist");
		}
		return registrationFormName;
	}

	public static DelegationComputerBuilder builder()
	{
		return new DelegationComputerBuilder();
	}

	public static final class DelegationComputerBuilder
	{
		private String fullGroupName;
		private String logoUrl;
		private List<String> readOnlyAttributes;
		private GroupDelegationConfigGenerator groupDelegationConfigGenerator;
		private RegistrationsManagement registrationsManagement;
		private EnquiryManagement enquiryManagement;

		private DelegationComputerBuilder()
		{
		}

		public DelegationComputerBuilder withFullGroupName(String fullGroupName)
		{
			this.fullGroupName = fullGroupName;
			return this;
		}

		public DelegationComputerBuilder withLogoUrl(String logoUrl)
		{
			this.logoUrl = logoUrl;
			return this;
		}

		public DelegationComputerBuilder withReadOnlyAttributes(List<String> readOnlyAttributes)
		{
			this.readOnlyAttributes = readOnlyAttributes;
			return this;
		}

		public DelegationComputerBuilder withGroupDelegationConfigGenerator(GroupDelegationConfigGenerator groupDelegationConfigGenerator)
		{
			this.groupDelegationConfigGenerator = groupDelegationConfigGenerator;
			return this;
		}

		public DelegationComputerBuilder withRegistrationsManagement(RegistrationsManagement registrationsManagement)
		{
			this.registrationsManagement = registrationsManagement;
			return this;
		}

		public DelegationComputerBuilder withEnquiryManagement(EnquiryManagement enquiryManagement)
		{
			this.enquiryManagement = enquiryManagement;
			return this;
		}

		public DelegationComputer build()
		{
			return new DelegationComputer(fullGroupName, logoUrl, readOnlyAttributes, groupDelegationConfigGenerator, registrationsManagement, enquiryManagement);
		}
	}
}
