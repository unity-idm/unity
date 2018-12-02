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
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupStructuralData;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupContents;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupMember;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.engine.api.utils.CodeGenerator;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.VerifiableEmail;

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
	private AttributesManagement attrMan;
	private AttributeTypeManagement attrTypeMan;
	private UnityMessageSource msg;
	private AttributesHelper attrHelper;
	private AttributeTypeHelper atHelper;
	private EntityManagement identitiesMan;

	@Autowired
	public DelegatedGroupManagementImpl(UnityMessageSource msg, @Qualifier("insecure") GroupsManagement groupMan,
			@Qualifier("insecure") BulkGroupQueryService bulkQueryService,
			@Qualifier("insecure") AttributesManagement attrMan,
			@Qualifier("insecure") AttributeTypeManagement attrTypeMan,
			@Qualifier("insecure") EntityManagement identitiesMan, AttributesHelper attrHelper,
			AttributeTypeHelper atHelper, ProjectAuthorizationManager authz)
	{
		this.msg = msg;
		this.groupMan = groupMan;
		this.bulkQueryService = bulkQueryService;
		this.authz = authz;
		this.attrMan = attrMan;
		this.attrTypeMan = attrTypeMan;
		this.attrHelper = attrHelper;
		this.atHelper = atHelper;
		this.identitiesMan = identitiesMan;
	}

	@Override
	@Transactional
	public void addGroup(String projectPath, String parentPath, I18nString groupName, boolean isOpen)
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
		toAdd.setOpen(isOpen);
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
								orgGroup.isOpen(), getGroupDisplayName(orgGroup)),
								content.getSubGroups()));
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
						orgGroup.isOpen(), getGroupDisplayName(orgGroup)),
				orgGroupContents.getSubGroups());

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
	public void setGroupAccessMode(String projectPath, String path, boolean isOpen) throws EngineException
	{
		authz.checkManagerAuthorization(projectPath, path);
		GroupContents groupContent = groupMan.getContents(path, GroupContents.METADATA | GroupContents.GROUPS);
		Group group = groupContent.getGroup();
		if (!isOpen)
		{
			assertIfChildsAreOpen(groupContent);
		} else
		{
			if (!projectPath.equals(path))
				assertIfParentIsClose(group);

		}

		group.setOpen(isOpen);
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
				Optional<String> val = getAttributeValue(entityId, gr.getName(),
						ProjectAuthorizationRoleAttributeTypeProvider.PROJECT_MANAGEMENT_AUTHORIZATION_ROLE);
				if (val.isPresent() && val.get().equals(GroupAuthorizationRole.manager.toString()))
				{
					projects.add(new DelegatedGroup(gr.toString(), gr.getDelegationConfiguration(),
							gr.isOpen(), getGroupDisplayName(gr)));

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
			for (GroupMembership member : orgGroupContents.getMembers())
			{
				long entity = member.getEntityId();
				DelegatedGroupMember entry = new DelegatedGroupMember(member.getEntityId(), projectPath,
						member.getGroup(), getGroupAuthRoleAttr(entity, projectPath),
						getAttributeFromMeta(entity, projectPath,
								EntityNameMetadataProvider.NAME),
						getAttributeFromMeta(entity, projectPath,
								ContactEmailMetadataProvider.NAME),
						getProjectMemberAttributes(entity, projectPath, projectAttrs));
				members.add(entry);
			}
		}
		return members;
	}

	private Group getGroupInternal(String path) throws EngineException
	{
		return groupMan.getContents(path, GroupContents.METADATA).getGroup();
	}

	private void assertIfChildsAreOpen(GroupContents group) throws EngineException
	{
		for (String child : group.getSubGroups())
		{
			Group childGroup = getGroupInternal(child);
			if (childGroup.isOpen())
			{
				throw new NotOpenChildGroupException(group.getGroup().getDisplayedName().getValue(msg),
						childGroup.getDisplayedName().getValue(msg));

			}
		}
	}

	private void assertIfParentIsClose(Group group) throws EngineException
	{
		if (!group.isTopLevel())
		{
			Group parent = getGroupInternal(group.getParentPath());
			if (!parent.isOpen())
			{
				throw new ParentIsCloseGroupException(parent.getDisplayedName().getValue(msg),
						group.getDisplayedName().getValue(msg));
			}
		}
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
			throw new OneManagerRemainsException(
					getGroupInternal(projectPath).getDisplayedName().getValue(msg));
	}

	private List<Attribute> getProjectMemberAttributes(long entity, String projectPath, List<String> attributes)
			throws EngineException
	{
		List<Attribute> ret = new ArrayList<>();
		if (attributes == null || attributes.isEmpty())
			return ret;
		for (String attr : attributes)
		{
			Optional<Attribute> oattr = getAttribute(entity, projectPath, attr);
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

	private Optional<Attribute> getAttribute(long entityId, String path, String attribute) throws EngineException
	{
		Collection<AttributeExt> attributes = attrMan.getAttributes(new EntityParam(entityId), path, attribute);

		if (!attributes.isEmpty())
		{
			return Optional.ofNullable(attributes.iterator().next());
		}
		return Optional.empty();
	}

	private Optional<String> getAttributeValue(long entityId, String path, String attribute) throws EngineException
	{

		Optional<Attribute> attr = getAttribute(entityId, path, attribute);
		if (attr.isPresent())
		{
			if (!attr.get().getValues().isEmpty())
			{
				return Optional.ofNullable(attr.get().getValues().iterator().next());
			}
		}
		return Optional.empty();
	}

	private GroupAuthorizationRole getGroupAuthRoleAttr(long entityId, String path) throws EngineException
	{
		Optional<String> attrVal = getAttributeValue(entityId, path,
				ProjectAuthorizationRoleAttributeTypeProvider.PROJECT_MANAGEMENT_AUTHORIZATION_ROLE);

		if (attrVal.isPresent())
			return GroupAuthorizationRole.valueOf(attrVal.get());

		return GroupAuthorizationRole.regular;

	}

	private String getAttributeFromMeta(long entityId, String path, String metadata) throws EngineException
	{
		AttributeType attrType = attrHelper.getAttributeTypeWithSingeltonMetadata(metadata);
		if (attrType == null)
			return null;

		Optional<String> value = getAttributeValue(entityId, path, attrType.getName());

		AttributeValueSyntax<?> syntax = atHelper.getUnconfiguredSyntaxForAttributeName(attrType.getName());

		if (value.isPresent())
		{
			if (syntax.isEmailVerifiable() && value.isPresent())
			{
				VerifiableEmail email = (VerifiableEmail) syntax.convertFromString(value.get());
				return email.getValue();
			}
			return value.get();
		} else
		{
			return null;
		}
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

	public static class NotOpenChildGroupException extends InternalException
	{
		public NotOpenChildGroupException(String parent, String child)
		{
			super("Cannot set group " + parent + " to close mode, child group " + child + " is open");
		}
	}

	public static class ParentIsCloseGroupException extends InternalException
	{
		public ParentIsCloseGroupException(String parent, String child)
		{
			super("Cannot set group " + child + " to open mode, parent group " + parent + " is open");
		}
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
