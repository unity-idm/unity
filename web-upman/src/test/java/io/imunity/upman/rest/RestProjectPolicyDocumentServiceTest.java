package io.imunity.upman.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.imunity.rest.api.types.policy.RestPolicyDocumentRequest;
import io.imunity.rest.api.types.policy.RestPolicyDocumentUpdateRequest;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.policy_document.PolicyDocumentContentType;
import pl.edu.icm.unity.base.registration.FormType;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentCreateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentUpdateRequest;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;

@ExtendWith(MockitoExtension.class)
public class RestProjectPolicyDocumentServiceTest
{
	@Mock
	private GroupsManagement groupMan;
	@Mock
	private GroupDelegationConfigGenerator groupDelegationConfigGenerator;
	@Mock
	private UpmanRestAuthorizationManager authz;
	@Mock
	private PolicyDocumentManagement policyDocumentManagement;
	@Mock
	private UpmanRestPolicyDocumentAuthorizationManager upmanRestPolicyDocumentAuthorizationManager;

	private RestProjectPolicyDocumentService restProjectService;

	@BeforeEach
	void setUp()
	{
		restProjectService = new RestProjectPolicyDocumentService(authz, groupDelegationConfigGenerator,
				policyDocumentManagement, groupMan, upmanRestPolicyDocumentAuthorizationManager, "/A", "/A/B");
	}

	@Test
	void shouldAddPolicyDocument() throws EngineException
	{

		when(policyDocumentManagement.addPolicyDocument(new PolicyDocumentCreateRequest("Policy1", new I18nString(),
				false, PolicyDocumentContentType.EMBEDDED, new I18nString()))).thenReturn(1L);
		GroupContents content = new GroupContents();
		Group group = new Group("/A");
		group.setDisplayedName(new I18nString());
		group.setDelegationConfiguration(new GroupDelegationConfiguration(true));
		content.setGroup(group);

		when(groupMan.getContents("/A/A", GroupContents.METADATA)).thenReturn(content);
		restProjectService.addPolicyDocument("A", RestPolicyDocumentRequest.builder()
				.withName("Policy1")
				.withMandatory(false)
				.withDisplayedName(Map.of())
				.withContent(Map.of())
				.withContentType(PolicyDocumentContentType.EMBEDDED.name())
				.build());
		GroupDelegationConfiguration groupDelegationConfiguration = new GroupDelegationConfiguration(false, false, null,
				null, null, null, null, List.of(1L));
		group.setDelegationConfiguration(groupDelegationConfiguration);
		verify(groupMan).updateGroup("/A/A", group);
	}

	@Test
	void shouldSynchronizeFormsAfterAddPolicyDocument() throws EngineException
	{

		when(policyDocumentManagement.addPolicyDocument(new PolicyDocumentCreateRequest("Policy1", new I18nString(),
				false, PolicyDocumentContentType.EMBEDDED, new I18nString()))).thenReturn(1L);
		GroupContents content = new GroupContents();
		Group group = new Group("/A");
		group.setDisplayedName(new I18nString());
		group.setDelegationConfiguration(
				new GroupDelegationConfiguration(false, false, null, "regForm", "enqForm", null, null, null));
		content.setGroup(group);

		when(groupMan.getContents("/A/A", GroupContents.METADATA)).thenReturn(content);
		restProjectService.addPolicyDocument("A", RestPolicyDocumentRequest.builder()
				.withName("Policy1")
				.withMandatory(false)
				.withDisplayedName(Map.of())
				.withContent(Map.of())
				.withContentType(PolicyDocumentContentType.EMBEDDED.name())
				.build());
		GroupDelegationConfiguration groupDelegationConfiguration = new GroupDelegationConfiguration(false, false, null,
				null, null, null, null, List.of(1L));
		group.setDelegationConfiguration(groupDelegationConfiguration);

		verify(groupDelegationConfigGenerator).resetFormsPolicies("regForm", FormType.REGISTRATION, List.of(1L));
		verify(groupDelegationConfigGenerator).resetFormsPolicies("enqForm", FormType.ENQUIRY, List.of(1L));

	}

	@Test
	void shouldRemovePolicyDocument() throws EngineException
	{
		GroupContents content = new GroupContents();
		Group group = new Group("/A");
		group.setDisplayedName(new I18nString());
		group.setDelegationConfiguration(
				new GroupDelegationConfiguration(false, false, null, null, null, null, null, List.of(1L)));
		content.setGroup(group);
		when(groupMan.getContents("/A/A", GroupContents.METADATA)).thenReturn(content);
		restProjectService.removePolicyDocument("A", 1L);
		verify(policyDocumentManagement).removePolicyDocument(1L);
		GroupDelegationConfiguration groupDelegationConfiguration = new GroupDelegationConfiguration(false, false, null,
				null, null, null, null, List.of());
		group.setDelegationConfiguration(groupDelegationConfiguration);
		verify(groupMan).updateGroup("/A/A", group);
	}

	@Test
	void shouldUpdatePolicyDocument() throws EngineException
	{
		GroupContents content = new GroupContents();
		Group group = new Group("/A");
		group.setDisplayedName(new I18nString());
		group.setDelegationConfiguration(
				new GroupDelegationConfiguration(false, false, null, null, null, null, null, List.of(1L)));
		content.setGroup(group);
		when(groupMan.getContents("/A/A", GroupContents.METADATA)).thenReturn(content);
		restProjectService.updatePolicyDocument("A", RestPolicyDocumentUpdateRequest.builder()
				.withName("name2")
				.withDisplayedName(Map.of())
				.withContent(Map.of("pl", "demo"))
				.withContentType(PolicyDocumentContentType.EMBEDDED.name())
				.withId(1L)
				.withMandatory(true)
				.build());
		verify(policyDocumentManagement).updatePolicyDocument(PolicyDocumentUpdateRequest.updateRequestBuilder()
				.withName("name2")
				.withId(1L)
				.withContentType(PolicyDocumentContentType.EMBEDDED.name())
				.withMandatory(true)
				.withDisplayedName(Map.of())
				.withContent(Map.of("pl", "demo"))
				.build());
	}

}
