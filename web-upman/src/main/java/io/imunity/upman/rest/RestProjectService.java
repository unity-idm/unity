/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import io.imunity.rest.api.types.policy.RestPolicyDocument;
import io.imunity.rest.api.types.policy.RestPolicyDocumentRequest;
import io.imunity.rest.api.types.policy.RestPolicyDocumentUpdateRequest;
import io.imunity.upman.rest.DelegationComputer.RollbackState;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.entity.EntityWithContactInfo;
import pl.edu.icm.unity.engine.api.group.GroupNotFoundException;
import pl.edu.icm.unity.engine.api.identity.UnknownEmailException;
import pl.edu.icm.unity.engine.api.idp.IdpPolicyAgreementContentChecker;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentCreateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentUpdateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupMember;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.engine.api.utils.CodeGenerator;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.UnknownIdentityException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.FormType;
import pl.edu.icm.unity.types.registration.RegistrationForm;

class RestProjectService
{
	private final DelegatedGroupManagement delGroupMan;
	private final GroupsManagement groupMan;
	private final GroupDelegationConfigGenerator groupDelegationConfigGenerator;
	private final UpmanRestAuthorizationManager authz;
	private final RegistrationsManagement registrationsManagement;
	private final EnquiryManagement enquiryManagement;
	private final EntityManagement idsMan;
	private final PolicyDocumentManagement policyDocumentManagement;
	private final List<IdpPolicyAgreementContentChecker> idpPolicyAgreementContentCheckers;
	
	private final String rootGroup;
	private final String authorizationGroup;


	public RestProjectService(DelegatedGroupManagement delGroupMan,
	                          GroupsManagement groupMan,
	                          GroupDelegationConfigGenerator groupDelegationConfigGenerator,
	                          RegistrationsManagement registrationsManagement,
	                          EnquiryManagement enquiryManagement,
	                          UpmanRestAuthorizationManager authz,
	                          EntityManagement idsMan,
	                          PolicyDocumentManagement policyDocumentManagement,
	                          List<IdpPolicyAgreementContentChecker> idpPolicyAgreementContentCheckers,
	                          String rootGroup,
	                          String authorizationGroup)
	{
		this.delGroupMan = delGroupMan;
		this.groupMan = groupMan;
		this.groupDelegationConfigGenerator = groupDelegationConfigGenerator;
		this.registrationsManagement = registrationsManagement;
		this.enquiryManagement = enquiryManagement;
		this.authz = authz;
		this.rootGroup = rootGroup;
		this.authorizationGroup = authorizationGroup;
		this.idsMan = idsMan;
		this.policyDocumentManagement = policyDocumentManagement;
		this.idpPolicyAgreementContentCheckers = idpPolicyAgreementContentCheckers;
	}

	@Transactional
	public RestProjectId addProject(RestProjectCreateRequest project) throws EngineException
	{
		assertAuthorization();

		String projectId;
		if(project.projectId == null)
		{
			GroupContents groupContent = groupMan.getContents(rootGroup, GroupContents.GROUPS);
			List<String> subGroups = groupContent.getSubGroups();
			projectId = generateName(subGroups);
		}
		else
			projectId = project.projectId;

		String projectPath = getProjectPath(projectId);
		if (project.displayedName == null || project.displayedName.isEmpty())
			throw new IllegalArgumentException();

		Group toAdd = new Group(projectPath);
		toAdd.setPublic(project.isPublic);
		toAdd.setDisplayedName(convertToI18nString(project.displayedName));
		toAdd.setDescription(convertToI18nString(project.description));

		groupMan.addGroup(toAdd);

		DelegationComputer delegationComputer = DelegationComputer.builder()
			.withFullGroupName(projectPath)
			.withLogoUrl(project.logoUrl)
			.withReadOnlyAttributes(project.readOnlyAttributes)
			.withGroupDelegationConfigGenerator(groupDelegationConfigGenerator)
			.withRegistrationsManagement(registrationsManagement)
			.withEnquiryManagement(enquiryManagement)
			.build();
		RollbackState rollbackState = delegationComputer.newRollbackState();
		try
		{
			String registrationFormName = delegationComputer.computeRegistrationFormName(project.registrationForm, rollbackState);
			String joinEnquiryName = delegationComputer.computeSignUpEnquiryName(project.signUpEnquiry, rollbackState);
			String updateEnquiryName = delegationComputer.computeMembershipUpdateEnquiryName(
					project.membershipUpdateEnquiry, rollbackState);
			toAdd.setDelegationConfiguration(new GroupDelegationConfiguration(true,
				project.enableSubprojects,
				project.logoUrl,
				registrationFormName, joinEnquiryName, updateEnquiryName,
				project.readOnlyAttributes, List.of())
			);

			groupMan.updateGroup(projectPath, toAdd);
		}
		catch (Exception e)
		{
			delegationComputer.rollback(rollbackState);
			groupMan.removeGroup(projectPath, false);
			throw e;
		}

		return new RestProjectId(projectId);
	}

	@Transactional
	public void updateProject(String projectId, RestProjectUpdateRequest project) throws EngineException
	{
		assertAuthorization();

		if (project.displayedName == null || project.displayedName.isEmpty())
			throw new IllegalArgumentException("Displayed name have to be set");

		String projectPath = getProjectPath(projectId);
		Group toUpdate = new Group(projectPath);
		toUpdate.setPublic(project.isPublic);
		toUpdate.setDisplayedName(convertToI18nString(project.displayedName));
		toUpdate.setDescription(convertToI18nString(project.description));

		DelegationComputer delegationComputer = DelegationComputer.builder()
			.withFullGroupName(projectPath)
			.withLogoUrl(project.logoUrl)
			.withReadOnlyAttributes(project.readOnlyAttributes)
			.withGroupDelegationConfigGenerator(groupDelegationConfigGenerator)
			.withRegistrationsManagement(registrationsManagement)
			.withEnquiryManagement(enquiryManagement)
			.build();
		RollbackState rollbackState = delegationComputer.newRollbackState();
		try
		{
			String registrationFormName = delegationComputer.computeRegistrationFormName(project.registrationForm, rollbackState);
			String joinEnquiryName = delegationComputer.computeSignUpEnquiryName(project.signUpEnquiry, rollbackState);
			String updateEnquiryName = delegationComputer.computeMembershipUpdateEnquiryName(project.membershipUpdateEnquiry, rollbackState);
			toUpdate.setDelegationConfiguration(new GroupDelegationConfiguration(true,
				project.enableSubprojects,
				project.logoUrl,
				registrationFormName, joinEnquiryName, updateEnquiryName,
				project.readOnlyAttributes, List.of())
			);
			groupMan.updateGroup(projectPath, toUpdate);
		}
		catch (GroupNotFoundException e)
		{
			throw new NotFoundException(e);
		}
	}

	private I18nString convertToI18nString(Map<String, String> map)
	{
		I18nString i18nString = new I18nString();
		i18nString.addAllValues(map);
		ofNullable(map.get("")).ifPresent(i18nString::setDefaultValue);
		return i18nString;
	}

	private static String generateName(List<String> subGroups)
	{
		String name;
		do
		{
			name = CodeGenerator.generateMixedCharCode(5);
		} while (subGroups.contains(name));
		return name;
	}

	@Transactional
	public void removeProject(String projectId) throws EngineException
	{
		assertAuthorization();
		try
		{
			groupMan.removeGroup(getProjectPath(projectId), true);
		}
		catch (GroupNotFoundException e)
		{
			throw new NotFoundException(e);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	@Transactional
	public RestProject getProject(String projectId) throws EngineException
	{
		assertAuthorization();
		GroupContents contents;
		try
		{
			contents = groupMan.getContents(getProjectPath(projectId),
				GroupContents.GROUPS | GroupContents.METADATA);
		}
		catch (GroupNotFoundException e)
		{
			throw new NotFoundException(e);
		}

		Group group = contents.getGroup();
		return map(projectId, group);
	}

	@Transactional
	public List<RestProject> getProjects() throws EngineException
	{
		assertAuthorization();
		List<Group> groups = groupMan.getGroupsByWildcard(rootGroup + "/**");
		return groups.stream()
			.filter(group -> !group.getName().equals(rootGroup))
			.filter(group -> group.getDelegationConfiguration().enabled)
			.map(group -> map(group.getName().replace(rootGroup + "/", ""), group))
			.collect(Collectors.toList());
	}

	@Transactional
	public void addProjectMember(String projectId, String email) throws EngineException
	{
		assertAuthorization();
		validateGroupPresence(projectId);
		Long id = getId(email);
		String projectPath = getProjectPath(projectId);
		delGroupMan.addMemberToGroup(projectPath, projectPath, id);
	}

	private void validateGroupPresence(String projectId) throws AuthorizationException
	{
		if (!groupMan.isPresent(getProjectPath(projectId)))
			throw new NotFoundException(String.format("Project %s doesn't exist", projectId));
	}

	private void validateRole(RestAuthorizationRole role)
	{
		if (Arrays.stream(GroupAuthorizationRole.values()).noneMatch(authzRole -> authzRole.name().equals(role.role)))
			throw new IllegalArgumentException(String.format("Invalid role: %s, allowed values: manager, " +
				"projectsAdmin, regular", role.role));
	}

	@Transactional
	public void removeProjectMember(String projectId, String email) throws EngineException
	{
		assertAuthorization();
		validateGroupPresence(projectId);
		try
		{
			groupMan.removeMember(getProjectPath(projectId), new EntityParam(new IdentityTaV(EmailIdentity.ID, email)));
		}
		catch (UnknownIdentityException e)
		{
			throw new NotFoundException(String.format("Email %s not found", email));
		}
	}

	@Transactional
	public List<RestProjectMembership> getProjectMembers(String projectId) throws EngineException
	{
		assertAuthorization();
		validateGroupPresence(projectId);
		String projectPath = getProjectPath(projectId);
		return delGroupMan.getDelegatedGroupMembers(projectPath, projectPath).stream()
			.map(RestProjectService::map)
			.collect(Collectors.toList());
	}

	@Transactional
	public RestProjectMembership getProjectMember(String projectId, String email) throws EngineException
	{
		assertAuthorization();
		validateGroupPresence(projectId);
		String projectPath = getProjectPath(projectId);
		return delGroupMan.getDelegatedGroupMembers(projectPath, projectPath).stream()
			.filter(member -> member.email.getValue().equals(email))
			.map(RestProjectService::map)
			.findFirst()
			.orElseThrow(() -> new NotFoundException("There is no member"));
	}

	@Transactional
	public RestAuthorizationRole getProjectAuthorizationRole(String projectId, String email) throws EngineException
	{
		return new RestAuthorizationRole(getProjectMember(projectId, email).role);
	}

	@Transactional
	public void setProjectAuthorizationRole(String projectId, String email,
	                                                         RestAuthorizationRole role) throws EngineException
	{
		assertAuthorization();
		validateGroupPresence(projectId);
		validateRole(role);
		Long id = getId(email);
		try
		{
			String projectPath = getProjectPath(projectId);
			delGroupMan.setGroupAuthorizationRole(projectPath, projectPath, id,
				GroupAuthorizationRole.valueOf(role.role));
		}
		catch (IllegalGroupValueException e)
		{
			throw new NotFoundException("Entity is not a member of the project");
		}
	}

	private Long getId(String email) throws EngineException
	{
		Set<EntityWithContactInfo> entities;
		try
		{
			entities = idsMan.getAllEntitiesWithContactEmails(Set.of(email));
		}
		catch (UnknownEmailException e)
		{
			throw new NotFoundException(String.format("Email %s not found", email));
		}

		if(entities.size() > 1)
			throw new BadRequestException("Ambiguous user");
		if(entities.size() == 0)
			throw new NotFoundException(String.format("Email %s not found", email));
		return entities.iterator().next().entity.getId();
	}

	private String getProjectPath(String projectId)
	{
		if(projectId.contains("/"))
			throw new IllegalArgumentException("Project Id cannot start form /");
		return rootGroup + "/" + projectId;
	}

	private void assertAuthorization() throws AuthorizationException
	{
		authz.assertManagerAuthorization(authorizationGroup);
	}

	private static RestProject map(String projectId, Group group)
	{
		return RestProject.builder()
			.withProjectId(projectId)
			.withPublic(group.isPublic())
			.withDisplayedName(group.getDisplayedName().getMap())
			.withDescription(group.getDescription().getMap())
			.withLogoUrl(group.getDelegationConfiguration().logoUrl)
			.withEnableSubprojects(group.getDelegationConfiguration().enableSubprojects)
			.withReadOnlyAttributes(group.getDelegationConfiguration().attributes)
			.withRegistrationForm(group.getDelegationConfiguration().registrationForm)
			.withSignUpEnquiry(group.getDelegationConfiguration().signupEnquiryForm)
			.withMembershipUpdateEnquiry(group.getDelegationConfiguration().membershipUpdateEnquiryForm)
			.build();
	}

	private static RestProjectMembership map(DelegatedGroupMember member)
	{
		return RestProjectMembership.builder()
			.withEmail(member.email.getValue())
			.withRole(member.role.name())
			.withAttributes(
				member.attributes.stream()
					.map(attr -> new RestAttribute(attr.getName(), List.copyOf(attr.getValues())))
					.collect(Collectors.toList())
			)
			.build();
	}

	public List<RestPolicyDocument> getPolicyDocuments(String projectId) throws EngineException {
		assertAuthorization();

		GroupContents groupContent = groupMan.getContents(getProjectPath(projectId), GroupContents.METADATA);
		Map<Long, PolicyDocumentWithRevision> policyDocuments = policyDocumentManagement.getPolicyDocuments().stream()
				.collect(Collectors.toMap(p -> p.id, p -> p));

		return groupContent.getGroup().getDelegationConfiguration().policyDocumentsIds.stream()
				.map(p -> policyDocuments.get(p)).map(RestProjectService::mapPolicy).collect(Collectors.toList());
	}

	public RestPolicyDocument getPolicyDocument(String projectId, Long policyId) throws EngineException {
		assertAuthorization();
		GroupContents groupContent = groupMan.getContents(getProjectPath(projectId), GroupContents.METADATA);
		assertGetProjectPolicyAuthorization(groupContent.getGroup().getDelegationConfiguration(), policyId);
		return mapPolicy(policyDocumentManagement.getPolicyDocument(policyId));
	}
	
	private void assertGetProjectPolicyAuthorization(GroupDelegationConfiguration groupDelegationConfiguration,
			Long policyId) throws AuthorizationException {
		if (!groupDelegationConfiguration.policyDocumentsIds.contains(policyId)) {
			throw new AuthorizationException(
					"Access to policy document is denied. The policy document is not in project scope.");
		}
	}

	private static RestPolicyDocument mapPolicy(PolicyDocumentWithRevision policyDocument) {
		return RestPolicyDocument.builder().withId(policyDocument.id).withName(policyDocument.name)
				.withDisplayedName(policyDocument.displayedName.getMap()).withMandatory(policyDocument.mandatory)
				.withContentType(policyDocument.contentType.name()).withRevision(policyDocument.revision)
				.withContent(policyDocument.content.getMap()).build();
	}

	public void removePolicyDocument(String projectId, Long policyId) throws EngineException {
		assertAuthorization();
		GroupContents groupContent = groupMan.getContents(getProjectPath(projectId), GroupContents.METADATA);
		Group group = groupContent.getGroup();
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		assertUpdateOrRemoveProjectPolicyAuthorization(group, policyId);
		
		List<Long> updatedPolicies = groupDelegationConfiguration.policyDocumentsIds.stream().filter(p -> !p.equals(policyId))
				.collect(Collectors.toList());
		
		GroupDelegationConfiguration updatedGroupDelegationConfiguration = new GroupDelegationConfiguration(groupDelegationConfiguration.enabled,
				groupDelegationConfiguration.enableSubprojects, groupDelegationConfiguration.logoUrl,
				groupDelegationConfiguration.registrationForm, groupDelegationConfiguration.signupEnquiryForm,
				groupDelegationConfiguration.membershipUpdateEnquiryForm, groupDelegationConfiguration.attributes,
				updatedPolicies);
		group.setDelegationConfiguration(updatedGroupDelegationConfiguration);
		groupMan.updateGroup(getProjectPath(projectId), group);
		policyDocumentManagement.removePolicyDocument(policyId);
		synchronizeForms(updatedGroupDelegationConfiguration);
	}

	public void updatePolicyDocument(String projectId, RestPolicyDocumentUpdateRequest policy) throws EngineException {
		assertAuthorization();
		assertUpdateOrRemoveProjectPolicyAuthorization(groupMan.getContents(getProjectPath(projectId), GroupContents.METADATA).getGroup(), policy.id);	
		policyDocumentManagement.updatePolicyDocument(mapPolicyDocumentRequest(policy));
	}
	
	private void assertUpdateOrRemoveProjectPolicyAuthorization(Group group,
			Long policyId) throws EngineException {
		
		if (group.getDelegationConfiguration().policyDocumentsIds == null)
		{
			throw new IllegalArgumentException("Policy with id " + policyId + "is unknown");
		}
		
		
		if (!group.getDelegationConfiguration().policyDocumentsIds.contains(policyId)) {
			throw new PolicyAuthorizationException();
		}
		
		assertIdPsContainsPolicyDocument(policyId);
		assertOtherGroupsContainsPolicyDocument(group, policyId);
		
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		List<RegistrationForm> regForms = new ArrayList<>(registrationsManagement.getForms());
		if (groupDelegationConfiguration.registrationForm != null)
		{
			regForms.removeIf(r -> r.getName().equals(groupDelegationConfiguration.registrationForm));
		}
		assertFormsContainsPolicyDocument(regForms, policyId);

		
		List<EnquiryForm> enqForms = new ArrayList<>(enquiryManagement.getEnquires());
		if (groupDelegationConfiguration.signupEnquiryForm != null)
		{
			enqForms.removeIf(r -> r.getName().equals(groupDelegationConfiguration.signupEnquiryForm));
		}
		if (groupDelegationConfiguration.membershipUpdateEnquiryForm != null)
		{
			enqForms.removeIf(r -> r.getName().equals(groupDelegationConfiguration.membershipUpdateEnquiryForm));
		}
		assertFormsContainsPolicyDocument(enqForms, policyId);
	}
	
	private void assertOtherGroupsContainsPolicyDocument(Group group,
			Long policyId) throws EngineException
	{
		Map<String, Group> allGroups = groupMan.getAllGroups();
		for (Group g : allGroups.values().stream()
				.filter(g -> !g.equals(group) && g.getDelegationConfiguration() != null).collect(Collectors.toList())) {
			if (g.getDelegationConfiguration().policyDocumentsIds != null
					&& g.getDelegationConfiguration().policyDocumentsIds.contains(policyId)) {
				throw new PolicyAuthorizationException();
			}
		}
	}
	
	private void assertIdPsContainsPolicyDocument(Long policyId) throws EngineException
	{
		for (IdpPolicyAgreementContentChecker idpChecker : idpPolicyAgreementContentCheckers)
		{
			if (idpChecker.isPolicyUsedOnEndpoints(policyId))
			{
				throw new PolicyAuthorizationException();
			}
		}
	}
	
	private void assertFormsContainsPolicyDocument(List<? extends BaseForm> forms, Long policyId) throws AuthorizationException
	{
		for (BaseForm form : forms)
		{
			if (form.getPolicyAgreements().stream().map(p -> p.documentsIdsToAccept).flatMap(Collection::stream)
					.anyMatch(s -> s.equals(policyId))) {
				throw new AuthorizationException(
						"Access to policy document is denied. The policy document is used in other context.");
			}
		}
	}
	
	public void addPolicyDocument(String projectId, RestPolicyDocumentRequest policy) throws EngineException {
		assertAuthorization();
		long addedPolicyDocument = policyDocumentManagement.addPolicyDocument(mapPolicyDocumentRequest(policy));
		GroupContents groupContent = groupMan.getContents(getProjectPath(projectId), GroupContents.METADATA);
		Group group = groupContent.getGroup();
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		List<Long> updatedPolicies = new ArrayList<>();
		if (groupDelegationConfiguration.policyDocumentsIds != null)
		{
			updatedPolicies.addAll(groupDelegationConfiguration.policyDocumentsIds);
		}
		
		updatedPolicies.add(addedPolicyDocument);
		
		GroupDelegationConfiguration updatedGroupDelegationConfiguration = new GroupDelegationConfiguration(groupDelegationConfiguration.enabled,
				groupDelegationConfiguration.enableSubprojects, groupDelegationConfiguration.logoUrl,
				groupDelegationConfiguration.registrationForm, groupDelegationConfiguration.signupEnquiryForm,
				groupDelegationConfiguration.membershipUpdateEnquiryForm, groupDelegationConfiguration.attributes,
				updatedPolicies);
		group.setDelegationConfiguration(updatedGroupDelegationConfiguration);
		groupMan.updateGroup(getProjectPath(projectId), group);
		synchronizeForms(updatedGroupDelegationConfiguration);
	}
	
	private void synchronizeForms(GroupDelegationConfiguration groupDelegationConfiguration)
			throws EngineException {
		if (groupDelegationConfiguration.registrationForm != null) {
			groupDelegationConfigGenerator.synchronizePolicy(groupDelegationConfiguration.registrationForm,
					FormType.REGISTRATION, groupDelegationConfiguration.policyDocumentsIds);
		}

		if (groupDelegationConfiguration.signupEnquiryForm != null) {
			groupDelegationConfigGenerator.synchronizePolicy(groupDelegationConfiguration.signupEnquiryForm,
					FormType.ENQUIRY, groupDelegationConfiguration.policyDocumentsIds);
		}
	}
	
	private static PolicyDocumentCreateRequest mapPolicyDocumentRequest(RestPolicyDocumentRequest restRequest) {
		return PolicyDocumentCreateRequest.createRequestBuilder().withName(restRequest.name)
				.withContentType(restRequest.contentType).withContent(restRequest.content)
				.withDisplayedName(restRequest.displayedName).withMandatory(restRequest.mandatory).build();
	}
	
	private static PolicyDocumentUpdateRequest mapPolicyDocumentRequest(RestPolicyDocumentUpdateRequest restRequest) {
		return PolicyDocumentUpdateRequest.updateRequestBuilder().withId(restRequest.id).withName(restRequest.name)
				.withContentType(restRequest.contentType).withContent(restRequest.content)
				.withDisplayedName(restRequest.displayedName).withMandatory(restRequest.mandatory).build();
	}
	
	private class PolicyAuthorizationException extends AuthorizationException {
		public PolicyAuthorizationException() {
			super("Access to policy document is denied. The policy document is also used in other than this project context.");

		}
	}
}
