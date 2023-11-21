package io.imunity.upman.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.idp.IdpPolicyAgreementContentChecker;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.RegistrationForm;

@Component
class UpmanRestPolicyDocumentAuthorizationManager
{

	private final RegistrationsManagement registrationsManagement;
	private final EnquiryManagement enquiryManagement;
	private final List<IdpPolicyAgreementContentChecker> idpPolicyAgreementContentCheckers;
	private final GroupsManagement groupMan;

	UpmanRestPolicyDocumentAuthorizationManager(RegistrationsManagement registrationsManagement,
			EnquiryManagement enquiryManagement,
			List<IdpPolicyAgreementContentChecker> idpPolicyAgreementContentCheckers, GroupsManagement groupMan)
	{
		this.registrationsManagement = registrationsManagement;
		this.enquiryManagement = enquiryManagement;
		this.idpPolicyAgreementContentCheckers = idpPolicyAgreementContentCheckers;
		this.groupMan = groupMan;
	}

	 void assertGetProjectPolicyAuthorization(GroupDelegationConfiguration groupDelegationConfiguration,
			Long policyId) throws AuthorizationException
	{
		if (!groupDelegationConfiguration.policyDocumentsIds.contains(policyId))
		{
			throw new AuthorizationException(
					"Access to policy document is denied. The policy document is not in project scope.");
		}
	}
	
	void assertUpdateOrRemoveProjectPolicyAuthorization(Group group, Long policyId) throws EngineException
	{
		if (group.getDelegationConfiguration().policyDocumentsIds == null
				|| !group.getDelegationConfiguration().policyDocumentsIds.contains(policyId))
		{
			throw new AuthorizationException(
					"Access to policy document is denied. The policy document is not in project scope.");
		}

		assertIdPsContainsPolicyDocument(policyId);
		assertOtherGroupsContainsPolicyDocument(group, policyId);

		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		List<RegistrationForm> regForms = new ArrayList<>(registrationsManagement.getForms());
		if (groupDelegationConfiguration.registrationForm != null)
		{
			regForms.removeIf(r -> r.getName()
					.equals(groupDelegationConfiguration.registrationForm));
		}
		assertFormsContainsPolicyDocument(regForms, policyId);

		List<EnquiryForm> enqForms = new ArrayList<>(enquiryManagement.getEnquires());
		if (groupDelegationConfiguration.signupEnquiryForm != null)
		{
			enqForms.removeIf(r -> r.getName()
					.equals(groupDelegationConfiguration.signupEnquiryForm));
		}
		if (groupDelegationConfiguration.membershipUpdateEnquiryForm != null)
		{
			enqForms.removeIf(r -> r.getName()
					.equals(groupDelegationConfiguration.membershipUpdateEnquiryForm));
		}
		assertFormsContainsPolicyDocument(enqForms, policyId);
	}

	void assertOtherGroupsContainsPolicyDocument(Group group, Long policyId) throws EngineException
	{
		Map<String, Group> allGroups = groupMan.getAllGroups();
		for (Group g : allGroups.values()
				.stream()
				.filter(g -> !g.equals(group) && g.getDelegationConfiguration() != null)
				.collect(Collectors.toList()))
		{
			if (g.getDelegationConfiguration().policyDocumentsIds != null
					&& g.getDelegationConfiguration().policyDocumentsIds.contains(policyId))
			{
				throw new PolicyAuthorizationException();
			}
		}
	}

	void assertIdPsContainsPolicyDocument(Long policyId) throws EngineException
	{
		for (IdpPolicyAgreementContentChecker idpChecker : idpPolicyAgreementContentCheckers)
		{
			if (idpChecker.isPolicyUsedOnEndpoints(policyId))
			{
				throw new PolicyAuthorizationException();
			}
		}
	}

	void assertFormsContainsPolicyDocument(List<? extends BaseForm> forms, Long policyId) throws AuthorizationException
	{
		for (BaseForm form : forms)
		{
			if (form.getPolicyAgreements()
					.stream()
					.map(p -> p.documentsIdsToAccept)
					.flatMap(Collection::stream)
					.anyMatch(s -> s.equals(policyId)))
			{
				throw new AuthorizationException(
						"Access to policy document is denied. The policy document is used in other context.");
			}
		}
	}

	private class PolicyAuthorizationException extends AuthorizationException
	{
		public PolicyAuthorizationException()
		{
			super("Access to policy document is denied. The policy document is also used in other than this project context.");

		}
	}
}
