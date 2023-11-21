package io.imunity.upman.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.imunity.rest.api.types.policy.RestPolicyDocument;
import io.imunity.rest.api.types.policy.RestPolicyDocumentRequest;
import io.imunity.rest.api.types.policy.RestPolicyDocumentUpdateRequest;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.registration.FormType;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentCreateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentUpdateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;

class RestProjectPolicyDocumentService
{
	private final UpmanRestAuthorizationManager authz;
	private final GroupDelegationConfigGenerator groupDelegationConfigGenerator;
	private final PolicyDocumentManagement policyDocumentManagement;
	private final GroupsManagement groupMan;
	private final UpmanRestPolicyDocumentAuthorizationManager restPolicyDocumentAuthorizationManager;

	private final String rootGroup;
	private final String authorizationGroup;

	RestProjectPolicyDocumentService(UpmanRestAuthorizationManager authz,
			GroupDelegationConfigGenerator groupDelegationConfigGenerator,
			PolicyDocumentManagement policyDocumentManagement, GroupsManagement groupMan,
			UpmanRestPolicyDocumentAuthorizationManager restPolicyDocumentAuthorizationManager, String rootGroup,
			String authorizationGroup)
	{
		this.authz = authz;
		this.groupDelegationConfigGenerator = groupDelegationConfigGenerator;
		this.policyDocumentManagement = policyDocumentManagement;
		this.groupMan = groupMan;
		this.restPolicyDocumentAuthorizationManager = restPolicyDocumentAuthorizationManager;
		this.rootGroup = rootGroup;
		this.authorizationGroup = authorizationGroup;
	}

	@Transactional
	public List<RestPolicyDocument> getPolicyDocuments(String projectId) throws EngineException
	{
		assertAuthorization();

		GroupContents groupContent = groupMan.getContents(ProjectPathProvider.getProjectPath(projectId, rootGroup),
				GroupContents.METADATA);
		Map<Long, PolicyDocumentWithRevision> policyDocuments = policyDocumentManagement.getPolicyDocuments()
				.stream()
				.collect(Collectors.toMap(p -> p.id, p -> p));

		return groupContent.getGroup()
				.getDelegationConfiguration().policyDocumentsIds.stream()
						.map(p -> policyDocuments.get(p))
						.map(RestProjectPolicyDocumentService::mapPolicy)
						.collect(Collectors.toList());
	}

	@Transactional
	public RestPolicyDocument getPolicyDocument(String projectId, Long policyId) throws EngineException
	{
		assertAuthorization();
		GroupContents groupContent = groupMan.getContents(ProjectPathProvider.getProjectPath(projectId, rootGroup),
				GroupContents.METADATA);
		restPolicyDocumentAuthorizationManager.assertGetProjectPolicyAuthorization(groupContent.getGroup()
				.getDelegationConfiguration(), policyId);
		return mapPolicy(policyDocumentManagement.getPolicyDocument(policyId));
	}

	private static RestPolicyDocument mapPolicy(PolicyDocumentWithRevision policyDocument)
	{
		return RestPolicyDocument.builder()
				.withId(policyDocument.id)
				.withName(policyDocument.name)
				.withDisplayedName(policyDocument.displayedName.getMap())
				.withMandatory(policyDocument.mandatory)
				.withContentType(policyDocument.contentType.name())
				.withRevision(policyDocument.revision)
				.withContent(policyDocument.content.getMap())
				.build();
	}

	@Transactional
	public void removePolicyDocument(String projectId, Long policyId) throws EngineException
	{
		assertAuthorization();
		GroupContents groupContent = groupMan.getContents(ProjectPathProvider.getProjectPath(projectId, rootGroup),
				GroupContents.METADATA);
		Group group = groupContent.getGroup();
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		restPolicyDocumentAuthorizationManager.assertUpdateOrRemoveProjectPolicyAuthorization(group, policyId);

		List<Long> updatedPolicies = groupDelegationConfiguration.policyDocumentsIds.stream()
				.filter(p -> !p.equals(policyId))
				.collect(Collectors.toList());

		GroupDelegationConfiguration updatedGroupDelegationConfiguration = new GroupDelegationConfiguration(
				groupDelegationConfiguration.enabled, groupDelegationConfiguration.enableSubprojects,
				groupDelegationConfiguration.logoUrl, groupDelegationConfiguration.registrationForm,
				groupDelegationConfiguration.signupEnquiryForm,
				groupDelegationConfiguration.membershipUpdateEnquiryForm, groupDelegationConfiguration.attributes,
				updatedPolicies);
		group.setDelegationConfiguration(updatedGroupDelegationConfiguration);
		groupMan.updateGroup(ProjectPathProvider.getProjectPath(projectId, rootGroup), group);
		policyDocumentManagement.removePolicyDocument(policyId);
		synchronizeForms(updatedGroupDelegationConfiguration);
	}

	@Transactional
	public void updatePolicyDocument(String projectId, RestPolicyDocumentUpdateRequest policy) throws EngineException
	{
		assertAuthorization();
		restPolicyDocumentAuthorizationManager.assertUpdateOrRemoveProjectPolicyAuthorization(
				groupMan.getContents(ProjectPathProvider.getProjectPath(projectId, rootGroup), GroupContents.METADATA)
						.getGroup(),
				policy.id);
		policyDocumentManagement.updatePolicyDocument(mapPolicyDocumentRequest(policy));
	}

	public void addPolicyDocument(String projectId, RestPolicyDocumentRequest policy) throws EngineException
	{
		assertAuthorization();
		long addedPolicyDocument = policyDocumentManagement.addPolicyDocument(mapPolicyDocumentRequest(policy));
		GroupContents groupContent = groupMan.getContents(ProjectPathProvider.getProjectPath(projectId, rootGroup),
				GroupContents.METADATA);
		Group group = groupContent.getGroup();
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		List<Long> updatedPolicies = new ArrayList<>();
		if (groupDelegationConfiguration.policyDocumentsIds != null)
		{
			updatedPolicies.addAll(groupDelegationConfiguration.policyDocumentsIds);
		}

		updatedPolicies.add(addedPolicyDocument);

		GroupDelegationConfiguration updatedGroupDelegationConfiguration = new GroupDelegationConfiguration(
				groupDelegationConfiguration.enabled, groupDelegationConfiguration.enableSubprojects,
				groupDelegationConfiguration.logoUrl, groupDelegationConfiguration.registrationForm,
				groupDelegationConfiguration.signupEnquiryForm,
				groupDelegationConfiguration.membershipUpdateEnquiryForm, groupDelegationConfiguration.attributes,
				updatedPolicies);
		group.setDelegationConfiguration(updatedGroupDelegationConfiguration);
		groupMan.updateGroup(ProjectPathProvider.getProjectPath(projectId, rootGroup), group);
		synchronizeForms(updatedGroupDelegationConfiguration);
	}

	private void synchronizeForms(GroupDelegationConfiguration groupDelegationConfiguration) throws EngineException
	{
		if (groupDelegationConfiguration.registrationForm != null)
		{
			groupDelegationConfigGenerator.synchronizePolicy(groupDelegationConfiguration.registrationForm,
					FormType.REGISTRATION, groupDelegationConfiguration.policyDocumentsIds);
		}

		if (groupDelegationConfiguration.signupEnquiryForm != null)
		{
			groupDelegationConfigGenerator.synchronizePolicy(groupDelegationConfiguration.signupEnquiryForm,
					FormType.ENQUIRY, groupDelegationConfiguration.policyDocumentsIds);
		}
	}

	private static PolicyDocumentCreateRequest mapPolicyDocumentRequest(RestPolicyDocumentRequest restRequest)
	{
		return PolicyDocumentCreateRequest.createRequestBuilder()
				.withName(restRequest.name)
				.withContentType(restRequest.contentType)
				.withContent(restRequest.content)
				.withDisplayedName(restRequest.displayedName)
				.withMandatory(restRequest.mandatory)
				.build();
	}

	private static PolicyDocumentUpdateRequest mapPolicyDocumentRequest(RestPolicyDocumentUpdateRequest restRequest)
	{
		return PolicyDocumentUpdateRequest.updateRequestBuilder()
				.withId(restRequest.id)
				.withName(restRequest.name)
				.withContentType(restRequest.contentType)
				.withContent(restRequest.content)
				.withDisplayedName(restRequest.displayedName)
				.withMandatory(restRequest.mandatory)
				.build();
	}

	private void assertAuthorization() throws AuthorizationException
	{
		authz.assertManagerAuthorization(authorizationGroup);
	}

	@Component
	public static class RestProjectPolicyDocumentServiceFactory
	{
		private final UpmanRestAuthorizationManager authz;
		private final GroupDelegationConfigGenerator groupDelegationConfigGenerator;
		private final PolicyDocumentManagement policyDocumentManagement;
		private final GroupsManagement groupMan;
		private final UpmanRestPolicyDocumentAuthorizationManager restPolicyDocumentAuthorizationManager;

		@Autowired
		RestProjectPolicyDocumentServiceFactory(UpmanRestAuthorizationManager authz,
				@Qualifier("insecure") GroupDelegationConfigGenerator groupDelegationConfigGenerator,
				@Qualifier("insecure") PolicyDocumentManagement policyDocumentManagement,
				@Qualifier("insecure") GroupsManagement groupMan,
				UpmanRestPolicyDocumentAuthorizationManager restPolicyDocumentAuthorizationManager)
		{
			this.authz = authz;
			this.groupDelegationConfigGenerator = groupDelegationConfigGenerator;
			this.policyDocumentManagement = policyDocumentManagement;
			this.groupMan = groupMan;
			this.restPolicyDocumentAuthorizationManager = restPolicyDocumentAuthorizationManager;
		}

		public RestProjectPolicyDocumentService newInstance(String rootGroup, String authorizeGroup)
		{
			return new RestProjectPolicyDocumentService(authz, groupDelegationConfigGenerator, policyDocumentManagement,
					groupMan, restPolicyDocumentAuthorizationManager, rootGroup, authorizeGroup);
		}
	}
}
