package io.imunity.upman.rest;

import io.imunity.rest.api.types.policy.RestPolicyDocument;
import io.imunity.rest.api.types.policy.RestPolicyDocumentRequest;
import io.imunity.rest.api.types.policy.RestPolicyDocumentUpdateRequest;
import io.imunity.rest.mappers.policy.PolicyDocumentMapper;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.registration.FormType;
import pl.edu.icm.unity.base.tx.Transactional;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@PrototypeComponent
class RestProjectPolicyDocumentService
{
	private UpmanRestAuthorizationManager authz;
	private GroupDelegationConfigGenerator groupDelegationConfigGenerator;
	private PolicyDocumentManagement policyDocumentManagement;
	private GroupsManagement groupMan;
	private UpmanRestPolicyDocumentAuthorizationManager restPolicyDocumentAuthorizationManager;
	private String rootGroup;
	private String authorizationGroup;
	private ProjectGroupProvider projectGroupProvider;

	//for spring
	@SuppressWarnings("unused")
	private RestProjectPolicyDocumentService()
	{
	}

	RestProjectPolicyDocumentService(UpmanRestAuthorizationManager authz,
			GroupDelegationConfigGenerator groupDelegationConfigGenerator,
			PolicyDocumentManagement policyDocumentManagement, GroupsManagement groupMan,
			UpmanRestPolicyDocumentAuthorizationManager restPolicyDocumentAuthorizationManager, String rootGroup,
			String authorizationGroup)
	{
		init(authz, groupDelegationConfigGenerator, policyDocumentManagement, groupMan,
				restPolicyDocumentAuthorizationManager, rootGroup, authorizationGroup);
	}

	void init(UpmanRestAuthorizationManager authz,
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
		this.projectGroupProvider = new ProjectGroupProvider(groupMan);
	}

	@Transactional
	public List<RestPolicyDocument> getPolicyDocuments(String projectId) throws EngineException
	{
		assertAuthorization();

		String projectPath = ProjectPathProvider.getProjectPath(projectId, rootGroup);
		Group group = projectGroupProvider.getProjectGroup(projectId, projectPath);
		Map<Long, PolicyDocumentWithRevision> policyDocuments = policyDocumentManagement.getPolicyDocuments()
				.stream()
				.collect(Collectors.toMap(p -> p.id, p -> p));

		return group
				.getDelegationConfiguration().policyDocumentsIds.stream()
						.map(p -> policyDocuments.get(p))
						.map(PolicyDocumentMapper::map)
						.collect(Collectors.toList());
	}

	@Transactional
	public RestPolicyDocument getPolicyDocument(String projectId, Long policyId) throws EngineException
	{
		assertAuthorization();
		String projectPath = ProjectPathProvider.getProjectPath(projectId, rootGroup);
		Group group = projectGroupProvider.getProjectGroup(projectId, projectPath);
		restPolicyDocumentAuthorizationManager.assertGetProjectPolicyAuthorization(group
				.getDelegationConfiguration(), policyId);
		return PolicyDocumentMapper.map(policyDocumentManagement.getPolicyDocument(policyId));
	}

	@Transactional
	public void removePolicyDocument(String projectId, Long policyId) throws EngineException
	{
		assertAuthorization();
		String projectPath = ProjectPathProvider.getProjectPath(projectId, rootGroup);
		Group group = projectGroupProvider.getProjectGroup(projectId, projectPath);
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
	public void updatePolicyDocument(String projectId, RestPolicyDocumentUpdateRequest policy, boolean updateRevision) throws EngineException
	{
		assertAuthorization();
		String projectPath = ProjectPathProvider.getProjectPath(projectId, rootGroup);
		Group group = projectGroupProvider.getProjectGroup(projectId, projectPath);
		restPolicyDocumentAuthorizationManager.assertUpdateOrRemoveProjectPolicyAuthorization(
				group,
				policy.id);
		if (updateRevision)
		{
			policyDocumentManagement.updatePolicyDocumentWithRevision(PolicyDocumentMapper.map(policy));

		} else
		{
			policyDocumentManagement.updatePolicyDocument(PolicyDocumentMapper.map(policy));
		}
	}

	@Transactional
	public RestPolicyDocumentId addPolicyDocument(String projectId, RestPolicyDocumentRequest policy) throws EngineException
	{
		assertAuthorization();
		String projectPath = ProjectPathProvider.getProjectPath(projectId, rootGroup);
		Group group = projectGroupProvider.getProjectGroup(projectId, projectPath);
		long addedPolicyDocument = policyDocumentManagement.addPolicyDocument(PolicyDocumentMapper.map(policy));
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
		return new RestPolicyDocumentId(addedPolicyDocument);
	}

	private void synchronizeForms(GroupDelegationConfiguration groupDelegationConfiguration) throws EngineException
	{
		if (groupDelegationConfiguration.registrationForm != null)
		{
			groupDelegationConfigGenerator.resetFormsPolicies(groupDelegationConfiguration.registrationForm,
					FormType.REGISTRATION, groupDelegationConfiguration.policyDocumentsIds);
		}

		if (groupDelegationConfiguration.signupEnquiryForm != null)
		{
			groupDelegationConfigGenerator.resetFormsPolicies(groupDelegationConfiguration.signupEnquiryForm,
					FormType.ENQUIRY, groupDelegationConfiguration.policyDocumentsIds);
		}
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
		private final ObjectFactory<RestProjectPolicyDocumentService> factory;

		@Autowired
		RestProjectPolicyDocumentServiceFactory(UpmanRestAuthorizationManager authz,
				@Qualifier("insecure") GroupDelegationConfigGenerator groupDelegationConfigGenerator,
				@Qualifier("insecure") PolicyDocumentManagement policyDocumentManagement,
				@Qualifier("insecure") GroupsManagement groupMan,
				UpmanRestPolicyDocumentAuthorizationManager restPolicyDocumentAuthorizationManager,
				ObjectFactory<RestProjectPolicyDocumentService> factory)
		{
			this.authz = authz;
			this.groupDelegationConfigGenerator = groupDelegationConfigGenerator;
			this.policyDocumentManagement = policyDocumentManagement;
			this.groupMan = groupMan;
			this.restPolicyDocumentAuthorizationManager = restPolicyDocumentAuthorizationManager;
			this.factory = factory;
		}

		RestProjectPolicyDocumentService newInstance(String rootGroup, String authorizeGroup)
		{
			RestProjectPolicyDocumentService bean = factory.getObject();
			bean.init(authz, groupDelegationConfigGenerator, policyDocumentManagement,
					groupMan, restPolicyDocumentAuthorizationManager, rootGroup, authorizeGroup);
			return bean;
		}
	}
}
