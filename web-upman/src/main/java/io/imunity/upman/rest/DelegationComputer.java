/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.RegistrationForm;

class DelegationComputer
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_UPMAN, DelegationComputer.class);
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

	RollbackState newRollbackState()
	{
		return new RollbackStateImpl();
	}
	
	String computeMembershipUpdateEnquiryName(RestMembershipEnquiry membershipUpdateEnquiry, RollbackState rollbackState) throws EngineException
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
			addToRollback(updateEnquiryName, rollbackState);
		} else if (membershipUpdateEnquiry.name != null)
		{
			if(!enquiryManagement.hasForm(membershipUpdateEnquiry.name))
				throw new BadRequestException("Form named " + membershipUpdateEnquiry.name + " does not exist");
		}
		return updateEnquiryName;
	}

	String computeSignUpEnquiryName(RestSignUpEnquiry signUpEnquiry, RollbackState rollbackState) throws EngineException
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
			addToRollback(joinEnquiryName, rollbackState);
		}
		else if (signUpEnquiry.name != null)
		{
			if(!enquiryManagement.hasForm(signUpEnquiry.name))
				throw new BadRequestException("Form named " + signUpEnquiry.name + " does not exist");
		}
		return joinEnquiryName;
	}

	String computeRegistrationFormName(RestRegistrationForm registrationForm, RollbackState rollbackState) throws EngineException
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
			addToRollback(registrationFormName, rollbackState);
		}
		else if (registrationForm.name != null)
		{
			if(!registrationsManagement.hasForm(registrationForm.name))
				throw new BadRequestException("Form named " + registrationForm.name + " does not exist");
		}
		return registrationFormName;
	}

	private void addToRollback(String form, RollbackState rollbackState)
	{
		((RollbackStateImpl)rollbackState).formsToDelete.add(form);
	}

	void rollback(RollbackState rollbackState)
	{
		RollbackStateImpl state = (RollbackStateImpl) rollbackState;
		for (String form : state.formsToDelete)
		{
			try
			{
				registrationsManagement.removeForm(form, false);
			} catch (Exception e)
			{
				log.error("Can't remove auto-created form {}; likely the next operation will fail", form, e);
			}
		}
	}
	
	static DelegationComputerBuilder builder()
	{
		return new DelegationComputerBuilder();
	}

	static final class DelegationComputerBuilder
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
	
	interface RollbackState
	{
	}
	
	private static class RollbackStateImpl implements RollbackState
	{
		private final Set<String> formsToDelete = new HashSet<>();
	}
}
