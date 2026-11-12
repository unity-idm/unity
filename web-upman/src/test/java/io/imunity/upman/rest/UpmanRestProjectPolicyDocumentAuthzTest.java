package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.idp.IdpPolicyAgreementContentChecker;

@ExtendWith(MockitoExtension.class)
class UpmanRestProjectPolicyDocumentAuthzTest
{
	@Mock
	private GroupsManagement groupMan;
	@Mock
	private RegistrationsManagement registrationsManagement;
	@Mock
	private EnquiryManagement enquiryManagement;
	@Mock
	private IdpPolicyAgreementContentChecker idpChecker;

	private UpmanRestPolicyDocumentAuthorizationManager authzService;

	@BeforeEach
	void setUp()
	{
		authzService = new UpmanRestPolicyDocumentAuthorizationManager(registrationsManagement, enquiryManagement,
				List.of(idpChecker), groupMan);
	}

	@ParameterizedTest
	@MethodSource("policyUses")
	void shouldDetectExternalUsesAndBlockUpdates(PolicyUse testCase) throws EngineException
	{
		Group group = project("/A");
		switch (testCase.context())
		{
		case "group" -> when(groupMan.getAllGroups()).thenReturn(Map.of("/B", project("/B")));
		case "registration" -> when(registrationsManagement.getForms()).thenReturn(List.of(registration("external")));
		case "enquiry" -> when(enquiryManagement.getEnquires()).thenReturn(List.of(enquiry("external")));
		case "idp" -> when(idpChecker.isPolicyUsedOnEndpoints(1L)).thenReturn(true);
		default -> throw new IllegalArgumentException(testCase.context());
		}

		assertThat(authzService.isPolicyUsedOutsideProject(group, 1L)).isTrue();
		assertThatThrownBy(() -> authzService.assertUpdateProjectPolicyAuthorization(group, 1L))
				.isInstanceOf(AuthorizationException.class).hasMessageContaining("other context");
	}

	@Test
	void shouldAllowUpdatesWhenOnlyProjectAndItsFormsUsePolicy() throws EngineException
	{
		Group group = project("/A");
		when(groupMan.getAllGroups()).thenReturn(Map.of("/A", project("/A")));
		when(registrationsManagement.getForms()).thenReturn(List.of(registration("registration")));
		when(enquiryManagement.getEnquires()).thenReturn(List.of(enquiry("signup"), enquiry("membership")));

		assertThat(authzService.isPolicyUsedOutsideProject(group, 1L)).isFalse();
		assertThatCode(() -> authzService.assertUpdateProjectPolicyAuthorization(group, 1L)).doesNotThrowAnyException();
	}

	@Test
	void shouldRejectPolicyOutsideProjectBeforeCheckingUsage()
	{
		Group group = project("/A");

		assertThatThrownBy(() -> authzService.assertUpdateProjectPolicyAuthorization(group, 2L))
				.isInstanceOf(AuthorizationException.class).hasMessageContaining("not in project scope");
		verifyNoInteractions(groupMan, registrationsManagement, enquiryManagement, idpChecker);
	}

	@Test
	void shouldRejectPolicyWhenProjectHasNoPolicies()
	{
		Group group = new Group("/A");
		group.setDelegationConfiguration(new GroupDelegationConfiguration(true));

		assertThatThrownBy(() -> authzService.assertGetProjectPolicyAuthorization(group.getDelegationConfiguration(), 1L))
				.isInstanceOf(AuthorizationException.class).hasMessageContaining("not in project scope");
	}

	private Group project(String path)
	{
		Group group = new Group(path);
		group.setDelegationConfiguration(new GroupDelegationConfiguration(true, false, null,
				"registration", "signup", "membership", List.of(), List.of(1L)));
		return group;
	}

	private RegistrationForm registration(String name)
	{
		return new RegistrationFormBuilder().withName(name).withDefaultCredentialRequirement("cr")
				.withAddedPolicyAgreement(new PolicyAgreementConfiguration(List.of(1L), null, null)).build();
	}

	private EnquiryForm enquiry(String name)
	{
		return new EnquiryFormBuilder().withName(name).withType(EnquiryForm.EnquiryType.STICKY)
				.withTargetGroups(new String[] {"/"})
				.withAddedPolicyAgreement(new PolicyAgreementConfiguration(List.of(1L), null, null)).build();
	}

	private static Stream<PolicyUse> policyUses()
	{
		return Stream.of("group", "registration", "enquiry", "idp")
				.map(context -> PolicyUse.builder().withContext(context).build());
	}

	private record PolicyUse(String context)
	{
		static Builder builder()
		{
			return new Builder();
		}

		private static class Builder
		{
			private String context;

			Builder withContext(String context)
			{
				this.context = context;
				return this;
			}

			PolicyUse build()
			{
				return new PolicyUse(context);
			}
		}
	}
}
