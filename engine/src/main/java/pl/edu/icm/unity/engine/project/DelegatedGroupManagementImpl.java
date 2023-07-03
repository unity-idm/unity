/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.verifiable.VerifiableElementBase;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupStructuralData;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupContents;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupMember;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.engine.api.project.SubprojectGroupDelegationConfiguration;
import pl.edu.icm.unity.engine.api.utils.CodeGenerator;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.store.api.tx.Transactional;

/**
 * Implementation of {@link DelegatedGroupManagement}
 * 
 * @author P.Piernik
 *
 */

@Component
@Primary
public class DelegatedGroupManagementImpl implements DelegatedGroupManagement
{
	private GroupsManagement groupMan;
	private BulkGroupQueryService bulkQueryService;
	private ProjectAuthorizationManager authz;
	private AttributeTypeManagement attrTypeMan;
	private MessageSource msg;
	private AttributesHelper attrHelper;
	private EntityManagement identitiesMan;
	private ProjectAttributeHelper projectAttrHelper;
	private GroupDelegationConfigGenerator groupDelegationConfigGenerator;
	private RegistrationsManagement registrationsManagement;
	private EnquiryManagement enquiryManagement;
	
	@Autowired
	public DelegatedGroupManagementImpl(MessageSource msg, @Qualifier("insecure") GroupsManagement groupMan,
			@Qualifier("insecure") BulkGroupQueryService bulkQueryService,
			@Qualifier("insecure") AttributeTypeManagement attrTypeMan,
			@Qualifier("insecure") EntityManagement identitiesMan, AttributesHelper attrHelper,
			@Qualifier("insecure") RegistrationsManagement registrationsManagement,	
			@Qualifier("insecure") EnquiryManagement enquiryManagement,
			GroupDelegationConfigGenerator groupDelegationConfigGenerator,
			ProjectAttributeHelper projectAttrHelper, ProjectAuthorizationManager authz)
	{
		
		this.msg = msg;
		this.authz = authz;
		this.groupMan = groupMan;
		this.identitiesMan = identitiesMan;
		this.bulkQueryService = bulkQueryService;	
		this.attrTypeMan = attrTypeMan;
		this.attrHelper = attrHelper;
		this.projectAttrHelper = projectAttrHelper;
		this.groupDelegationConfigGenerator = groupDelegationConfigGenerator;
		this.registrationsManagement = registrationsManagement;
		this.enquiryManagement = enquiryManagement;
	}

	@Override
	@Transactional
	public String addGroup(String projectPath, String parentPath, I18nString groupName, boolean isPublic)
			throws EngineException
	{
		authz.assertManagerAuthorization(projectPath, parentPath);
		GroupContents groupContent = groupMan.getContents(parentPath, GroupContents.GROUPS);
		List<String> subGroups = groupContent.getSubGroups();

		if (groupName == null || groupName.isEmpty())
			throw new IllegalGroupNameException();

		String name;
		do
		{
			name = CodeGenerator.generateMixedCharCode(5);
		} while (subGroups.contains(name));

		Group toAdd = new Group(new Group(parentPath), name);
		toAdd.setPublic(isPublic);
		toAdd.setDisplayedName(groupName);
		groupMan.addGroup(toAdd);
		return toAdd.toString();
	}

	@Override
	@Transactional
	public void removeGroup(String projectPath, String path) throws EngineException
	{
		authz.assertManagerAuthorization(projectPath, path);
		if (projectPath.equals(path))
			throw new RemovalOfProjectGroupException(projectPath);
		
		Group group = getGroupInternal(path);
		GroupDelegationConfiguration delegationConfig = group.getDelegationConfiguration();
		if (delegationConfig.enabled)
		{
			throw new RemovalOfSubProjectGroupException(projectPath);
		}
		
		groupMan.removeGroup(path, true);
	}
	
	@Override
	@Transactional
	public void removeProject(String projectPath, String subProjectPath) throws EngineException
	{
		authz.assertProjectsAdminAuthorization(projectPath, subProjectPath);
		if (projectPath.equals(subProjectPath))
			throw new RemovalOfProjectGroupException(projectPath);
		
		Group group = getGroupInternal(subProjectPath);
		GroupDelegationConfiguration delegationConfig = group.getDelegationConfiguration();
		if (delegationConfig.enabled)
		{
			removeRelatedForms(group.getDelegationConfiguration());
		} 
		
		groupMan.removeGroup(subProjectPath, true);
	}
	
	private void removeRelatedForms(GroupDelegationConfiguration groupConfig) throws EngineException
	{
		if (!Strings.isNullOrEmpty(groupConfig.registrationForm))
		{
			registrationsManagement.removeFormWithoutDependencyChecking(groupConfig.registrationForm);
		}
		
		if (!Strings.isNullOrEmpty(groupConfig.signupEnquiryForm))
		{
			enquiryManagement.removeEnquiryWithoutDependencyChecking(groupConfig.signupEnquiryForm);
		}
		
		if (!Strings.isNullOrEmpty(groupConfig.membershipUpdateEnquiryForm))
		{
			enquiryManagement.removeEnquiryWithoutDependencyChecking(groupConfig.membershipUpdateEnquiryForm);
		}		
	}
	
	@Override
	@Transactional
	public Map<String, DelegatedGroupContents> getGroupAndSubgroups(String projectPath, String subgroupPath)
			throws EngineException
	{

		authz.assertManagerAuthorization(projectPath, subgroupPath);
		GroupStructuralData bulkData = bulkQueryService.getBulkStructuralData(subgroupPath);
		Map<String, GroupContents> groupAndSubgroups = bulkQueryService.getGroupAndSubgroups(bulkData);
		Map<String, DelegatedGroupContents> ret = new HashMap<>();
		for (Entry<String, GroupContents> entry : groupAndSubgroups.entrySet())
		{
			GroupContents content = entry.getValue();
			if (content != null)
			{
				Group orgGroup = content.getGroup();
				ret.put(entry.getKey(),
						new DelegatedGroupContents(new DelegatedGroup(orgGroup.toString(),
								orgGroup.getDelegationConfiguration(),
								orgGroup.isPublic(), orgGroup.getDisplayedName()),
								Optional.ofNullable(content.getSubGroups())));
			}
		}
		return ret;
	}

	@Override
	@Transactional
	public DelegatedGroupContents getContents(String projectPath, String subgroupPath) throws EngineException
	{
		authz.assertManagerAuthorization(projectPath, subgroupPath);
		GroupContents orgGroupContents = groupMan.getContents(subgroupPath,
				GroupContents.GROUPS | GroupContents.METADATA);
		Group orgGroup = orgGroupContents.getGroup();

		return new DelegatedGroupContents(
				new DelegatedGroup(orgGroup.toString(), orgGroup.getDelegationConfiguration(),
						orgGroup.isPublic(), orgGroup.getDisplayedName()),
				Optional.ofNullable(orgGroupContents.getSubGroups()));

	}

	@Override
	@Transactional
	public List<DelegatedGroupMember> getDelegatedGroupMembers(String projectPath, String subgroupPath)
			throws EngineException
	{
		authz.assertManagerAuthorization(projectPath, subgroupPath);
		return getDelegatedGroupMembersInternal(projectPath, subgroupPath);
	}

	@Override
	@Transactional
	public String getAttributeDisplayedName(String projectPath, String attrName) throws EngineException
	{
		authz.assertManagerAuthorization(projectPath);
		List<String> attrs = getProjectAttrs(projectPath);

		if (!attrs.contains(attrName))
		{
			throw new IllegalGroupAttributeException(attrName, projectPath);
		}
		return attrTypeMan.getAttributeType(attrName).getDisplayedName().getValue(msg);
	}

	@Override
	@Transactional
	public void setGroupDisplayedName(String projectPath, String path, I18nString newName) throws EngineException
	{
		authz.assertManagerAuthorization(projectPath, path);
		if (projectPath.equals(path))
			throw new RenameProjectGroupException(projectPath);

		Group group = getGroupInternal(path);
		group.setDisplayedName(newName);
		groupMan.updateGroup(path, group, "set displayed name", newName.getValue(msg));
	}

	@Override
	@Transactional
	public void setGroupAccessMode(String projectPath, String path, boolean isPublic) throws EngineException
	{
		authz.assertManagerAuthorization(projectPath, path);
		GroupContents groupContent = groupMan.getContents(path, GroupContents.METADATA | GroupContents.GROUPS);
		Group group = groupContent.getGroup();
		group.setPublic(isPublic);
		groupMan.updateGroup(path, group, "set access mode", isPublic ? "public" : "private");
	}

	@Override
	@Transactional
	public void setGroupAuthorizationRole(String projectPath, String subgroupPath, long entityId, GroupAuthorizationRole role)
			throws EngineException
	{
		authz.assertRoleManagerAuthorization(projectPath, subgroupPath, role);
		Attribute attr = new Attribute(
				ProjectAuthorizationRoleAttributeTypeProvider.PROJECT_MANAGEMENT_AUTHORIZATION_ROLE,
				null, subgroupPath, Lists.newArrayList(role.toString()));
		
		if (projectPath.equals(subgroupPath) && role.equals(GroupAuthorizationRole.regular))
		{
			assertIfOneManagerRemain(projectPath, entityId);
		}

		attrHelper.addSystemAttribute(entityId, attr, true);

	}

	@Override
	@Transactional
	public List<DelegatedGroup> getProjectsForEntity(long entityId) throws EngineException
	{
		List<DelegatedGroup> projects = new ArrayList<>();
		Map<String, GroupMembership> groups = identitiesMan.getGroups(new EntityParam(entityId));
		for (String group : groups.keySet())
		{
			Group gr = getGroupInternal(group);
			if (gr.getDelegationConfiguration().enabled)
			{
				Optional<String> val = projectAttrHelper.getAttributeValue(entityId, gr.getName(),
						ProjectAuthorizationRoleAttributeTypeProvider.PROJECT_MANAGEMENT_AUTHORIZATION_ROLE);
				if (val.isPresent() && !val.get().equals(GroupAuthorizationRole.regular.toString())) 
						
				{
					projects.add(new DelegatedGroup(gr.toString(), gr.getDelegationConfiguration(),
							gr.isPublic(), gr.getDisplayedName()));

				}
			}
		}
		return projects;
	}

	@Override
	@Transactional
	public void addMemberToGroup(String projectPath, String subgroupPath, long entityId) throws EngineException
	{
		authz.assertManagerAuthorization(projectPath, subgroupPath);
		final Deque<String> notMember = getMissingEntityGroups(subgroupPath, entityId);
		addToGroupRecursive(notMember, entityId);

	}

	@Override
	@Transactional
	public void removeMemberFromGroup(String projectPath, String subgroupPath, long entityId) throws EngineException
	{
		authz.assertManagerAuthorization(projectPath, subgroupPath);
		groupMan.removeMember(subgroupPath, new EntityParam(entityId));
	}
	
	@Override
	@Transactional
	public GroupAuthorizationRole getGroupAuthorizationRole(String projectPath, long entityId)
			throws EngineException
	{
		authz.assertManagerAuthorization(projectPath);
		Optional<String> val = projectAttrHelper.getAttributeValue(entityId, projectPath,
				ProjectAuthorizationRoleAttributeTypeProvider.PROJECT_MANAGEMENT_AUTHORIZATION_ROLE);
		
		return val.isPresent() ? GroupAuthorizationRole.valueOf(val.get()) : null;
	}

	@Override
	public void setGroupDelegationConfiguration(String projectPath, String subgroupPath, SubprojectGroupDelegationConfiguration subprojectDelegationConfiguration) throws EngineException
	{

		authz.assertProjectsAdminAuthorization(projectPath, subgroupPath);
		Group projectGroup = getGroupInternal(projectPath);
		GroupDelegationConfiguration projectDelConfig = projectGroup.getDelegationConfiguration();
		Group group = getGroupInternal(subgroupPath);

		GroupDelegationConfiguration groupDelegationConfig = group.getDelegationConfiguration();

		String registrationFormName = groupDelegationConfig.registrationForm;
		String joinEnquiryName = groupDelegationConfig.signupEnquiryForm;
		String updateEnquiryName = groupDelegationConfig.membershipUpdateEnquiryForm;

		if (subprojectDelegationConfiguration.enabled)
		{
			if (Strings.isNullOrEmpty(registrationFormName)
					&& !Strings.isNullOrEmpty(projectDelConfig.registrationForm))
			{
				RegistrationForm regForm = groupDelegationConfigGenerator
						.generateSubprojectRegistrationForm(projectDelConfig.registrationForm,
								projectPath, subgroupPath,
								subprojectDelegationConfiguration.logoUrl);
				registrationsManagement.addForm(regForm);
				registrationFormName = regForm.getName();
			}

			if (Strings.isNullOrEmpty(joinEnquiryName)
					&& !Strings.isNullOrEmpty(projectDelConfig.signupEnquiryForm))
			{
				EnquiryForm joinEnquiryForm = groupDelegationConfigGenerator
						.generateSubprojectJoinEnquiryForm(projectDelConfig.signupEnquiryForm,
								projectPath, subgroupPath,
								subprojectDelegationConfiguration.logoUrl);
				enquiryManagement.addEnquiry(joinEnquiryForm);
				joinEnquiryName = joinEnquiryForm.getName();

			}
			if (Strings.isNullOrEmpty(updateEnquiryName)
					&& !Strings.isNullOrEmpty(projectDelConfig.membershipUpdateEnquiryForm))
			{
				EnquiryForm updateEnquiryForm = groupDelegationConfigGenerator
						.generateSubprojectUpdateEnquiryForm(
								projectDelConfig.membershipUpdateEnquiryForm,
								projectPath, subgroupPath,
								subprojectDelegationConfiguration.logoUrl);
				enquiryManagement.addEnquiry(updateEnquiryForm);
				updateEnquiryName = updateEnquiryForm.getName();
			}
		}

		group.setDelegationConfiguration(new GroupDelegationConfiguration(subprojectDelegationConfiguration.enabled,
				subprojectDelegationConfiguration.enableSubprojects, 
				subprojectDelegationConfiguration.logoUrl,
				registrationFormName, joinEnquiryName, updateEnquiryName,
				projectDelConfig.attributes));
		groupMan.updateGroup(subgroupPath, group);
	}
	
	private List<DelegatedGroupMember> getDelegatedGroupMembersInternal(String projectPath, String path)
			throws EngineException
	{
		GroupContents orgGroupContents = groupMan.getContents(path, GroupContents.MEMBERS);
		List<GroupMembership> orgMembers = orgGroupContents.getMembers();
		List<DelegatedGroupMember> members = new ArrayList<>();
		if (orgMembers != null && !orgMembers.isEmpty())
		{

			List<String> projectAttrs = getProjectAttrs(projectPath);
			for (GroupMembership member : orgMembers)
			{
				long entity = member.getEntityId();
				VerifiableElementBase emailId = getEmailIdentity(entity);
				DelegatedGroupMember entry = new DelegatedGroupMember(member.getEntityId(), projectPath,
						member.getGroup(), getGroupAuthRoleAttr(entity, path),
						projectAttrHelper.getAttributeFromMeta(entity, "/",
								EntityNameMetadataProvider.NAME),
						emailId != null ? emailId : projectAttrHelper.getVerifiableAttributeFromMeta(entity, "/",
								ContactEmailMetadataProvider.NAME),
						Optional.ofNullable(getProjectMemberAttributes(entity, projectPath, projectAttrs)));
				members.add(entry);
			}
		}
		members.sort((m1,m2) -> Long.compare(m1.entityId, m2.entityId));
		return members;
	}

	private Group getGroupInternal(String path) throws EngineException
	{
		return groupMan.getContents(path, GroupContents.METADATA).getGroup();
	}

	

	private void assertIfOneManagerRemain(String projectPath, long entityId) throws EngineException
	{

		List<DelegatedGroupMember> delegatedGroupMemebersInternal = getDelegatedGroupMembersInternal(
				projectPath, projectPath);
		List<Long> managers = new ArrayList<>();

		for (DelegatedGroupMember member : delegatedGroupMemebersInternal)
		{
			if (!member.role.equals(GroupAuthorizationRole.regular))
			{
				managers.add(member.entityId);
			}
		}

		if (managers.size() == 1 && managers.contains(entityId))
			throw new OneManagerRemainsException(projectPath);
	}

	private List<Attribute> getProjectMemberAttributes(long entity, String projectPath, List<String> attributes)
			throws EngineException
	{
		List<Attribute> ret = new ArrayList<>();
		if (attributes == null || attributes.isEmpty())
			return ret;
		for (String attr : attributes)
		{
			Optional<Attribute> oattr = projectAttrHelper.getAttribute(entity, projectPath, attr);
			if (oattr.isPresent())
				ret.add(oattr.get());

		}
		return ret;
	}

	private List<String> getProjectAttrs(String projectPath) throws EngineException
	{
		Group projectGroup = getGroupInternal(projectPath);
		return projectGroup.getDelegationConfiguration().attributes;
	}

	private GroupAuthorizationRole getGroupAuthRoleAttr(long entityId, String path) throws EngineException
	{
		Optional<String> attrVal = projectAttrHelper.getAttributeValue(entityId, path,
				ProjectAuthorizationRoleAttributeTypeProvider.PROJECT_MANAGEMENT_AUTHORIZATION_ROLE);

		if (attrVal.isPresent())
			return GroupAuthorizationRole.valueOf(attrVal.get());

		return GroupAuthorizationRole.regular;

	}

	private Deque<String> getMissingEntityGroups(String finalGroup, long entityId) throws EngineException
	{
		EntityParam entityParam = new EntityParam(entityId);
		Collection<String> existingGroups;
		existingGroups = identitiesMan.getGroups(entityParam).keySet();
		final Deque<String> notMember = Group.getMissingGroups(finalGroup, existingGroups);
		return notMember;
	}

	private void addToGroupRecursive(final Deque<String> notMember, long entity) throws EngineException
	{
		if (notMember.isEmpty())
			return;
		String current = notMember.pollLast();
		groupMan.addMemberFromParent(current, new EntityParam(entity));
		addToGroupRecursive(notMember, entity);
	}
	
	private VerifiableElementBase getEmailIdentity(long entityId) throws EngineException
	{
		Entity entity = identitiesMan.getEntity(new EntityParam(entityId));
		for (IdentityParam id : entity.getIdentities())
		{
			if (id != null && id.getTypeId().equals(EmailIdentity.ID))
				return new VerifiableElementBase(id.getValue(), id.getConfirmationInfo());

		}
		return null;
	}

	public static class IllegalGroupAttributeException extends InternalException
	{
		public IllegalGroupAttributeException(String attributeName, String projectPath)
		{
			super("Attribute " + attributeName
					+ " is not definded as read only attribute in project group (" + projectPath
					+ ") configuration");
		}
	}

	public static class OneManagerRemainsException extends InternalException
	{
		public OneManagerRemainsException(String group)
		{
			super("At least one manager should remains in group " + group);
		}
	}

	public static class RenameProjectGroupException extends InternalException
	{
		public RenameProjectGroupException(String group)
		{
			super("Can not rename the main project group " + group);
		}
	}

	public static class RemovalOfProjectGroupException extends InternalException
	{
		public RemovalOfProjectGroupException(String group)
		{
			super("Can not remove the main project group " + group);
		}
	}
	
	public static class RemovalOfSubProjectGroupException extends InternalException
	{
		public RemovalOfSubProjectGroupException(String group)
		{
			super("Can not remove the sub-project group " + group);
		}
	}

	public static class IllegalGroupNameException extends InternalException
	{
		public IllegalGroupNameException()
		{
			super("Group name can not be empty or null");
		}
	}
}
