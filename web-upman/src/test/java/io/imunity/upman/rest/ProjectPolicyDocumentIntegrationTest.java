package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import io.imunity.rest.api.types.policy.RestPolicyDocument;
import io.imunity.rest.api.types.policy.RestPolicyDocumentRequest;
import io.imunity.rest.api.types.policy.RestPolicyDocumentUpdateRequest;
import io.imunity.upman.rest.RestProjectPolicyDocumentService.RestProjectPolicyDocumentServiceFactory;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.policy_document.PolicyDocumentContentType;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.authn.AuthorizationExceptionRT;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.InvocationContext.InvocationMaterial;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentNotFoundException;
import pl.edu.icm.unity.engine.api.project.RestProjectAuthorization;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

class ProjectPolicyDocumentIntegrationTest extends DBIntegrationTestBase
{
	private static final String PROJECT = "/projects/alpha";

	@Autowired
	private RestProjectPolicyDocumentServiceFactory policies;
	@Autowired
	private PolicyDocumentManagement policyDocuments;
	@Autowired
	private GroupDelegationConfigGenerator formGenerator;
	@Autowired
	private EnquiryManagement enquiries;

	private RestProjectPolicyDocumentService rest;
	private Identity caller;
	private String registrationName;
	private String enquiryName;

	@BeforeEach
	void configureProject() throws Exception
	{
		attrsMan.createAttribute(new EntityParam(InvocationContext.getCurrent().getLoginSession().getEntityId()),
				new Attribute(RestProjectAuthorization.AUTHORIZATION_ROLE, "enumeration", "/", List.of("manager")));
		groupsMan.addGroup(new Group("/projects"));
		Group project = project(PROJECT, List.of());
		groupsMan.addGroup(project);
		RegistrationForm registration = formGenerator.generateProjectRegistrationForm(PROJECT, null, List.of(), List.of());
		registrationsMan.addForm(registration);
		registrationName = registration.getName();
		EnquiryForm enquiry = formGenerator.generateProjectJoinEnquiryForm(PROJECT, null, List.of());
		enquiries.addEnquiry(enquiry);
		enquiryName = enquiry.getName();
		project.setDelegationConfiguration(GroupDelegationConfiguration.builder().copy(project.getDelegationConfiguration())
				.withRegistrationForm(registrationName).withSignupEnquiryForm(enquiryName).build());
		groupsMan.updateGroup(PROJECT, project);
		caller = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "policy-caller"),
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, EntityState.valid);
		rest = policies.newInstance("/projects", "/");
	}

	@ParameterizedTest
	@MethodSource("accessCases")
	void shouldCreateUpdateAndDeletePoliciesWithoutGlobalRights(AccessCase testCase) throws Exception
	{
		InvocationContext admin = useCaller(testCase);
		assertThatThrownBy(() -> groupsMan.getAllGroups()).isInstanceOf(AuthorizationExceptionRT.class);

		long id = addPolicy();
		RestPolicyDocument original = rest.getPolicyDocument("alpha", id);
		rest.updatePolicyDocument("alpha", update(id, "updated"), true);
		RestPolicyDocument revised = rest.getPolicyDocument("alpha", id);
		assertThat(revised.revision).isEqualTo(original.revision + 1);
		assertThat(revised.content).containsEntry("en", "updated");
		rest.updatePolicyDocument("alpha", update(id, "corrected"), false);
		RestPolicyDocument corrected = rest.getPolicyDocument("alpha", id);
		assertThat(corrected.revision).isEqualTo(revised.revision);
		assertThat(corrected.content).containsEntry("en", "corrected");
		rest.removePolicyDocument("alpha", id);

		assertThat(rest.getPolicyDocuments("alpha")).isEmpty();
		InvocationContext.setCurrent(admin);
		assertThatThrownBy(() -> policyDocuments.getPolicyDocument(id))
				.isInstanceOf(PolicyDocumentNotFoundException.class);
		assertProjectFormsEmpty();
	}

	@ParameterizedTest
	@MethodSource("sharedPolicyCases")
	void shouldUnlinkSharedPolicyAndPreserveOtherUses(AccessCase testCase) throws Exception
	{
		long id = addPolicy();
		switch (testCase.sharedContext())
		{
		case "group" -> groupsMan.addGroup(project("/projects/beta", List.of(id)));
		case "registration" -> registrationsMan.addForm(formGenerator.generateProjectRegistrationForm(
				"/projects", null, List.of(), List.of(id)));
		case "enquiry" -> enquiries.addEnquiry(formGenerator.generateProjectJoinEnquiryForm(
				"/projects", null, List.of(id)));
		default -> throw new IllegalArgumentException(testCase.sharedContext());
		}
		RestPolicyDocument original = rest.getPolicyDocument("alpha", id);
		InvocationContext admin = useCaller(testCase);

		assertThatThrownBy(() -> rest.updatePolicyDocument("alpha", update(id, "denied"), true))
				.isInstanceOf(AuthorizationException.class).hasMessageContaining("other context");
		rest.removePolicyDocument("alpha", id);

		assertThat(rest.getPolicyDocuments("alpha")).isEmpty();
		assertThatThrownBy(() -> rest.getPolicyDocument("alpha", id)).isInstanceOf(AuthorizationException.class);
		InvocationContext.setCurrent(admin);
		assertThat(policyDocuments.getPolicyDocument(id).revision).isEqualTo(original.revision);
		assertThat(policyDocuments.getPolicyDocument(id).content.getMap()).isEqualTo(original.content);
		assertProjectFormsEmpty();
		switch (testCase.sharedContext())
		{
		case "group" -> assertThat(groupsMan.getContents("/projects/beta", GroupContents.METADATA)
				.getGroup().getDelegationConfiguration().policyDocumentsIds).containsExactly(id);
		case "registration" -> assertThat(registrationsMan.getForms().stream()
				.filter(form -> !form.getName().equals(registrationName))
				.flatMap(form -> form.getPolicyAgreements().stream())
				.flatMap(agreement -> agreement.documentsIdsToAccept.stream())).containsExactly(id);
		case "enquiry" -> assertThat(enquiries.getEnquires().stream()
				.filter(form -> !form.getName().equals(enquiryName))
				.flatMap(form -> form.getPolicyAgreements().stream())
				.flatMap(agreement -> agreement.documentsIdsToAccept.stream())).containsExactly(id);
		default -> throw new IllegalArgumentException(testCase.sharedContext());
		}
	}

	@ParameterizedTest
	@MethodSource("accessCases")
	void shouldRejectUpdatingOrDeletingPolicyOutsideProject(AccessCase testCase) throws Exception
	{
		long id = addPolicy();
		groupsMan.addGroup(project("/projects/beta", List.of()));
		useCaller(testCase);

		assertThatThrownBy(() -> rest.updatePolicyDocument("beta", update(id, "denied"), true))
				.isInstanceOf(AuthorizationException.class);
		assertThatThrownBy(() -> rest.removePolicyDocument("beta", id)).isInstanceOf(AuthorizationException.class);
		assertThat(rest.getPolicyDocument("alpha", id).content).containsEntry("en", "original");
	}

	private long addPolicy() throws Exception
	{
		return rest.addPolicyDocument("alpha", RestPolicyDocumentRequest.builder().withName("policy").withMandatory(false)
				.withContentType(PolicyDocumentContentType.EMBEDDED.name()).withDisplayedName(Map.of("en", "Policy"))
				.withContent(Map.of("en", "original")).build()).id;
	}

	private RestPolicyDocumentUpdateRequest update(long id, String content)
	{
		return RestPolicyDocumentUpdateRequest.builder().withId(id).withName("policy").withMandatory(false)
				.withContentType(PolicyDocumentContentType.EMBEDDED.name()).withDisplayedName(Map.of("en", "Policy"))
				.withContent(Map.of("en", content)).build();
	}

	private InvocationContext useCaller(AccessCase testCase) throws Exception
	{
		if (testCase.material() == InvocationMaterial.DIRECT)
		{
			attrsMan.createAttribute(new EntityParam(caller), new Attribute(RestProjectAuthorization.AUTHORIZATION_ROLE,
					"enumeration", "/", List.of("manager")));
		}
		InvocationContext admin = InvocationContext.getCurrent();
		InvocationContext context = new InvocationContext(null, null, List.of());
		context.setLoginSession(new LoginSession(null, null, 0, caller.getEntityId(), null, null, null, null));
		context.setInvocationMaterial(testCase.material());
		context.setScopes(testCase.scopes());
		InvocationContext.setCurrent(context);
		return admin;
	}

	private void assertProjectFormsEmpty() throws Exception
	{
		assertThat(registrationsMan.getForm(registrationName).getPolicyAgreements()).isEmpty();
		assertThat(enquiries.getEnquiry(enquiryName).getPolicyAgreements()).isEmpty();
	}

	private Group project(String path, List<Long> policyIds)
	{
		Group group = new Group(path);
		group.setDelegationConfiguration(new GroupDelegationConfiguration(true, true,
				null, null, null, null, List.of(), policyIds));
		return group;
	}

	private static Stream<AccessCase> accessCases()
	{
		return accessCases(null);
	}

	private static Stream<AccessCase> sharedPolicyCases()
	{
		return Stream.of("group", "registration", "enquiry").flatMap(ProjectPolicyDocumentIntegrationTest::accessCases);
	}

	private static Stream<AccessCase> accessCases(String sharedContext)
	{
		return Stream.of(
				AccessCase.builder().withMaterial(InvocationMaterial.DIRECT).withSharedContext(sharedContext).build(),
				AccessCase.builder().withMaterial(InvocationMaterial.OAUTH_DELEGATION)
						.withScopes("sys:upman-api").withSharedContext(sharedContext).build(),
				AccessCase.builder().withMaterial(InvocationMaterial.OAUTH_DELEGATION)
						.withScopes("sys:upman-api:alpha").withSharedContext(sharedContext).build());
	}

	private record AccessCase(InvocationMaterial material, List<String> scopes, String sharedContext)
	{
		static Builder builder()
		{
			return new Builder();
		}

		private static class Builder
		{
			private InvocationMaterial material;
			private List<String> scopes = List.of();
			private String sharedContext;

			Builder withMaterial(InvocationMaterial material)
			{
				this.material = material;
				return this;
			}

			Builder withScopes(String... scopes)
			{
				this.scopes = List.of(scopes);
				return this;
			}

			Builder withSharedContext(String sharedContext)
			{
				this.sharedContext = sharedContext;
				return this;
			}

			AccessCase build()
			{
				return new AccessCase(material, scopes, sharedContext);
			}
		}
	}
}
