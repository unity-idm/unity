package io.imunity.upman.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.idp.IdpPolicyAgreementContentChecker;

@Component
class UpmanRestPolicyDocumentAuthorizationManager
{

	private final RegistrationsManagement registrationsManagement;
	private final EnquiryManagement enquiryManagement;
	private final List<IdpPolicyAgreementContentChecker> idpPolicyAgreementContentCheckers;
	private final GroupsManagement groupMan;

	UpmanRestPolicyDocumentAuthorizationManager(@Qualifier("insecure") RegistrationsManagement registrationsManagement,
			@Qualifier("insecure") EnquiryManagement enquiryManagement,
			List<IdpPolicyAgreementContentChecker> idpPolicyAgreementContentCheckers,
			@Qualifier("insecure") GroupsManagement groupMan)
	{
		this.registrationsManagement = registrationsManagement;
		this.enquiryManagement = enquiryManagement;
		this.idpPolicyAgreementContentCheckers = idpPolicyAgreementContentCheckers;
		this.groupMan = groupMan;
	}

	void assertGetProjectPolicyAuthorization(GroupDelegationConfiguration groupDelegationConfiguration,
			Long policyId) throws AuthorizationException
	{
		if (groupDelegationConfiguration.policyDocumentsIds == null
				|| !groupDelegationConfiguration.policyDocumentsIds.contains(policyId))
		{
			throw new AuthorizationException(
					"Access to policy document is denied. The policy document is not in project scope.");
		}
	}

	void assertUpdateProjectPolicyAuthorization(Group group, Long policyId) throws EngineException
	{
		assertGetProjectPolicyAuthorization(group.getDelegationConfiguration(), policyId);
		if (isPolicyUsedOutsideProject(group, policyId))
		{
			throw new AuthorizationException(
					"Access to policy document is denied. The policy document is used in other context.");
		}
	}

	boolean isPolicyUsedOutsideProject(Group group, Long policyId) throws EngineException
	{
		if (isPolicyUsedOnEndpoints(policyId) || isPolicyUsedInOtherGroups(group, policyId))
		{
			return true;
		}

		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		List<RegistrationForm> regForms = new ArrayList<>(registrationsManagement.getForms());
		if (groupDelegationConfiguration.registrationForm != null)
		{
			regForms.removeIf(r -> r.getName()
					.equals(groupDelegationConfiguration.registrationForm));
		}
		if (formsContainPolicyDocument(regForms, policyId))
		{
			return true;
		}

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
		return formsContainPolicyDocument(enqForms, policyId);
	}

	private boolean isPolicyUsedInOtherGroups(Group group, Long policyId) throws EngineException
	{
		return groupMan.getAllGroups().values().stream()
				.filter(other -> !other.getPathEncoded().equals(group.getPathEncoded()))
				.map(Group::getDelegationConfiguration)
				.anyMatch(config -> config != null && config.policyDocumentsIds != null
						&& config.policyDocumentsIds.contains(policyId));
	}

	private boolean isPolicyUsedOnEndpoints(Long policyId) throws EngineException
	{
		for (IdpPolicyAgreementContentChecker idpChecker : idpPolicyAgreementContentCheckers)
		{
			if (idpChecker.isPolicyUsedOnEndpoints(policyId))
			{
				return true;
			}
		}
		return false;
	}

	private boolean formsContainPolicyDocument(List<? extends BaseForm> forms, Long policyId)
	{
		return forms.stream()
				.flatMap(form -> form.getPolicyAgreements().stream())
				.map(agreement -> agreement.documentsIdsToAccept)
				.flatMap(Collection::stream)
				.anyMatch(policyId::equals);
	}
}
