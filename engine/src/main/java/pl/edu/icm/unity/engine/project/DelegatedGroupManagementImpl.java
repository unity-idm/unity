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

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupStructuralData;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupContents;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupMember;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.engine.api.utils.CodeGenerator;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.VerifiableElementBase;

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
	private UnityMessageSource msg;
	private AttributesHelper attrHelper;
	private EntityManagement identitiesMan;
	private ProjectAttributeHelper projectAttrHelper;

	@Autowired
	public DelegatedGroupManagementImpl(UnityMessageSource msg, @Qualifier("insecure") GroupsManagement groupMan,
			@Qualifier("insecure") BulkGroupQueryService bulkQueryService,
			@Qualifier("insecure") AttributeTypeManagement attrTypeMan,
			@Qualifier("insecure") EntityManagement identitiesMan, AttributesHelper attrHelper,
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
	}

	@Override
	@Transactional
	public void addGroup(String projectPath, String parentPath, I18nString groupName, boolean isPublic)
			throws EngineException
	{
		authz.checkManagerAuthorization(projectPath, parentPath);
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
	}

	@Override
	@Transactional
	public void removeGroup(String projectPath, String path) throws EngineException
	{
		authz.checkManagerAuthorization(projectPath, path);
		if (projectPath.equals(path))
			throw new RemovalOfProjectGroupException(projectPath);
		groupMan.removeGroup(path, true);

	}

	@Override
	@Transactional
	public Map<String, DelegatedGroupContents> getGroupAndSubgroups(String projectPath, String path)
			throws EngineException
	{

		authz.checkManagerAuthorization(projectPath, path);
		GroupStructuralData bulkData = bulkQueryService.getBulkStructuralData(path);
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
								orgGroup.isPublic(), getGroupDisplayName(orgGroup)),
								Optional.ofNullable(content.getSubGroups())));
			}
		}
		return ret;
	}

	@Override
	@Transactional
	public DelegatedGroupContents getContents(String projectPath, String path) throws EngineException
	{
		authz.checkManagerAuthorization(projectPath, path);
		GroupContents orgGroupContents = groupMan.getContents(path,
				GroupContents.GROUPS | GroupContents.METADATA);
		Group orgGroup = orgGroupContents.getGroup();

		return new DelegatedGroupContents(
				new DelegatedGroup(orgGroup.toString(), orgGroup.getDelegationConfiguration(),
						orgGroup.isPublic(), getGroupDisplayName(orgGroup)),
				Optional.ofNullable(orgGroupContents.getSubGroups()));

	}

	@Override
	@Transactional
	public List<DelegatedGroupMember> getDelegatedGroupMemebers(String projectPath, String path)
			throws EngineException
	{
		authz.checkManagerAuthorization(projectPath, path);
		return getDelegatedGroupMemebersInternal(projectPath, path);
	}

	@Override
	@Transactional
	public String getAttributeDisplayedName(String projectPath, String attrName) throws EngineException
	{
		authz.checkManagerAuthorization(projectPath);
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
		authz.checkManagerAuthorization(projectPath, path);
		if (projectPath.equals(path))
			throw new RenameProjectGroupException(projectPath);

		Group group = getGroupInternal(path);
		group.setDisplayedName(newName);
		groupMan.updateGroup(path, group);
	}

	@Override
	@Transactional
	public void setGroupAccessMode(String projectPath, String path, boolean isPublic) throws EngineException
	{
		authz.checkManagerAuthorization(projectPath, path);
		GroupContents groupContent = groupMan.getContents(path, GroupContents.METADATA | GroupContents.GROUPS);
		Group group = groupContent.getGroup();
		group.setPublic(isPublic);
		groupMan.updateGroup(path, group);
	}

	@Override
	@Transactional
	public void setGroupAuthorizationRole(String projectPath, long entityId, GroupAuthorizationRole role)
			throws EngineException
	{
		authz.checkManagerAuthorization(projectPath);
		List<String> val = new ArrayList<>();
		val.add(role.toString());
		Attribute attr = new Attribute(
				ProjectAuthorizationRoleAttributeTypeProvider.PROJECT_MANAGEMENT_AUTHORIZATION_ROLE,
				null, projectPath, val);

		if (role.equals(GroupAuthorizationRole.regular))
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
				if (val.isPresent() && val.get().equals(GroupAuthorizationRole.manager.toString()))
				{
					projects.add(new DelegatedGroup(gr.toString(), gr.getDelegationConfiguration(),
							gr.isPublic(), getGroupDisplayName(gr)));

				}
			}
		}
		return projects;
	}

	@Override
	@Transactional
	public void addMemberToGroup(String projectPath, String groupPath, long entityId) throws EngineException
	{
		authz.checkManagerAuthorization(projectPath, groupPath);
		final Deque<String> notMember = getMissingEntityGroups(groupPath, entityId);
		addToGroupRecursive(notMember, entityId);

	}

	@Override
	@Transactional
	public void removeMemberFromGroup(String projectPath, String groupPath, long entityId) throws EngineException
	{
		authz.checkManagerAuthorization(projectPath, groupPath);
		groupMan.removeMember(groupPath, new EntityParam(entityId));
	}

	private List<DelegatedGroupMember> getDelegatedGroupMemebersInternal(String projectPath, String path)
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
						member.getGroup(), getGroupAuthRoleAttr(entity, projectPath),
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

		List<DelegatedGroupMember> delegatedGroupMemebersInternal = getDelegatedGroupMemebersInternal(
				projectPath, projectPath);
		List<Long> managers = new ArrayList<>();

		for (DelegatedGroupMember member : delegatedGroupMemebersInternal)
		{
			if (member.role.equals(GroupAuthorizationRole.manager))
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

	private String getGroupDisplayName(Group group)
	{
		String displayName = group.getDisplayedName().getValue(msg);

		if (group.getName().equals(displayName))
		{
			return group.getNameShort();
		}

		return displayName;
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

	public static class IllegalGroupNameException extends InternalException
	{
		public IllegalGroupNameException()
		{
			super("Group name can not be empty or null");
		}
	}
}
