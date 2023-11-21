package io.imunity.upman.rest;

import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.idp.IdpPolicyAgreementContentChecker;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;

@ExtendWith(MockitoExtension.class)
public class UpmanRestProjectPolicyDocumentAuthzTest
{
	@Mock
	private GroupsManagement groupMan;

	@Mock
	private RegistrationsManagement registrationsManagement;
	
	@Mock
	private EnquiryManagement enquiryManagement;

	@Mock
	private IdpPolicyAgreementContentChecker idpPolicyAgreementContentChecker;

	@Mock
	private PolicyDocumentManagement policyDocumentManagement;

	private UpmanRestPolicyDocumentAuthorizationManager authzService;

	@BeforeEach
	void setUp()
	{
		authzService = new UpmanRestPolicyDocumentAuthorizationManager(registrationsManagement, enquiryManagement,
				List.of(idpPolicyAgreementContentChecker), groupMan);
	}

	@Test
	void shouldBlockRemoveWhenPolicyDocumentIsConfiguredInAnotherGroup() throws EngineException
	{
		GroupContents content = new GroupContents();
		Group group = new Group("/A");
		group.setDisplayedName(new I18nString());
		group.setDelegationConfiguration(
				new GroupDelegationConfiguration(false, false, null, null, null, null, null, List.of(1L)));
		content.setGroup(group);

		Group groupB = new Group("/B");
		groupB.setDisplayedName(new I18nString());
		groupB.setDelegationConfiguration(
				new GroupDelegationConfiguration(false, false, null, null, null, null, null, List.of(1L)));

		when(groupMan.getAllGroups()).thenReturn(Map.of("/A", group, "/B", groupB));
		Assertions.assertThrows(AuthorizationException.class,
				() -> authzService.assertUpdateOrRemoveProjectPolicyAuthorization(group, 1L));
	}

	@Test
	void shouldBlockRemoveWhenPolicyDocumentIsConfiguredInAnotherForm() throws EngineException
	{
		GroupContents content = new GroupContents();
		Group group = new Group("/A");
		group.setDisplayedName(new I18nString());
		group.setDelegationConfiguration(
				new GroupDelegationConfiguration(false, false, null, "regForm", null, null, null, List.of(1L)));
		content.setGroup(group);

		when(registrationsManagement.getForms()).thenReturn(List.of(new RegistrationFormBuilder().withName("regForm2")
				.withDefaultCredentialRequirement("cr")
				.withAddedPolicyAgreement(new PolicyAgreementConfiguration(List.of(1L), null, null))
				.build()));
		Assertions.assertThrows(AuthorizationException.class,
				() -> authzService.assertUpdateOrRemoveProjectPolicyAuthorization(group, 1L));
	}

	@Test
	void shouldBlockRemoveWhenPolicyDocumentIsConfiguredInIdp() throws EngineException
	{
		GroupContents content = new GroupContents();
		Group group = new Group("/A");
		group.setDisplayedName(new I18nString());
		group.setDelegationConfiguration(
				new GroupDelegationConfiguration(false, false, null, null, null, null, null, List.of(1L)));
		content.setGroup(group);
		when(idpPolicyAgreementContentChecker.isPolicyUsedOnEndpoints(1L)).thenReturn(true);

		Assertions.assertThrows(AuthorizationException.class,
				() -> authzService.assertUpdateOrRemoveProjectPolicyAuthorization(group, 1L));
	}

	@Test
	void shouldBlockUpdateWhenPolicyDocumentIsConfiguredInAnotherGroup() throws EngineException
	{
		GroupContents content = new GroupContents();
		Group group = new Group("/A");
		group.setDisplayedName(new I18nString());
		group.setDelegationConfiguration(
				new GroupDelegationConfiguration(false, false, null, null, null, null, null, List.of(1L)));
		content.setGroup(group);

		Group groupB = new Group("/B");
		groupB.setDisplayedName(new I18nString());
		groupB.setDelegationConfiguration(
				new GroupDelegationConfiguration(false, false, null, null, null, null, null, List.of(1L)));

		when(groupMan.getAllGroups()).thenReturn(Map.of("/A", group, "/B", groupB));

		Assertions.assertThrows(AuthorizationException.class,
				() -> authzService.assertUpdateOrRemoveProjectPolicyAuthorization(group, 1L));
	}

	@Test
	void shouldBlockUpdateWhenPolicyDocumentIsConfiguredInAnotherForm() throws EngineException
	{
		GroupContents content = new GroupContents();
		Group group = new Group("/A");
		group.setDisplayedName(new I18nString());
		group.setDelegationConfiguration(
				new GroupDelegationConfiguration(false, false, null, "regForm", null, null, null, List.of(1L)));
		content.setGroup(group);

		when(registrationsManagement.getForms()).thenReturn(List.of(new RegistrationFormBuilder().withName("regForm2")
				.withDefaultCredentialRequirement("cr")
				.withAddedPolicyAgreement(new PolicyAgreementConfiguration(List.of(1L), null, null))
				.build()));
		Assertions.assertThrows(AuthorizationException.class,
				() -> authzService.assertUpdateOrRemoveProjectPolicyAuthorization(group, 1L));
	}

	@Test
	void shouldBlockUpdateWhenPolicyDocumentIsConfiguredInIdp() throws EngineException
	{
		GroupContents content = new GroupContents();
		Group group = new Group("/A");
		group.setDisplayedName(new I18nString());
		group.setDelegationConfiguration(
				new GroupDelegationConfiguration(false, false, null, null, null, null, null, List.of(1L)));
		content.setGroup(group);
		when(idpPolicyAgreementContentChecker.isPolicyUsedOnEndpoints(1L)).thenReturn(true);

		Assertions.assertThrows(AuthorizationException.class,
				() -> authzService.assertUpdateOrRemoveProjectPolicyAuthorization(group, 1L));
	}

}
