/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.group.delegation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupStructuralData;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.DelegatedGroupMember;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupAuthorizationRole;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.VerifiableEmail;

/**
 * Implementation of {@link DelegatedGroupManagement}
 * @author P.Piernik
 *
 */

@Component
@Primary
public class DelegatedGroupManagementImpl implements DelegatedGroupManagement
{

	private GroupsManagement groupMan;
	private BulkGroupQueryService bulkQueryService;
	private GroupAuthorizationManager authz;
	private AttributesManagement attrMan;
	private AttributeTypeManagement attrTypeMan;
	private UnityMessageSource msg;
	private AttributesHelper attrHelper;
	private AttributeTypeHelper atHelper;
	private EntityManagement identitiesMan;

	@Autowired
	public DelegatedGroupManagementImpl(UnityMessageSource msg,
			@Qualifier("insecure") GroupsManagement groupMan,
			@Qualifier("insecure") BulkGroupQueryService bulkQueryService,
			@Qualifier("insecure") AttributesManagement attrMan,
			@Qualifier("insecure") AttributeTypeManagement attrTypeMan,
			@Qualifier("insecure") EntityManagement identitiesMan,
			AttributesHelper attrHelper, AttributeTypeHelper atHelper,
			GroupAuthorizationManager authz)
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
	public void addGroup(String projectPath, Group toAdd) throws EngineException
	{
		authz.checkManagerAuthorization(projectPath);
		checkIsGroupChildren(projectPath, toAdd.getName());
		groupMan.addGroup(toAdd);

	}

	@Override
	@Transactional
	public void removeGroup(String projectPath, String path, boolean recursive)
			throws EngineException
	{
		authz.checkManagerAuthorization(projectPath);
		checkIsGroupChildren(projectPath, path);
		groupMan.removeGroup(path, recursive);

	}

	@Override
	@Transactional
	public Map<String, GroupContents> getGroupAndSubgroups(String projectPath, String path)
			throws EngineException
	{

		authz.checkManagerAuthorization(projectPath);
		checkIsGroupChildren(projectPath, path);
		GroupStructuralData bulkData = bulkQueryService.getBulkStructuralData(path);
		return bulkQueryService.getGroupAndSubgroups(bulkData);
	}

	@Override
	public Map<String, String> getAdditionalAttributeNamesForProject(String groupPath)
			throws EngineException
	{

		authz.checkManagerAuthorization(groupPath);
		Map<String, String> ret = new HashMap<>();

		AttributeType attr = null;
		if (groupPath.length() == 2)

			attr = attrTypeMan.getAttributeType("mobile");
		else
			attr = attrTypeMan.getAttributeType("firstname");

		ret.put(attr.getName(), attr.getDisplayedName().getValue(msg));

		return ret;
	}

	private Group getGroup(String path) throws EngineException
	{
		return groupMan.getContents(path, GroupContents.METADATA).getGroup();
	}

	@Override
	@Transactional
	public void setGroupDisplayedName(String projectPath, String path, I18nString newName)
			throws EngineException
	{
		authz.checkManagerAuthorization(projectPath);
		checkIsGroupChildren(projectPath, path);
		Group group = getGroup(path);
		group.setDisplayedName(newName);
		groupMan.updateGroup(path, group);
	}

	@Override
	@Transactional
	public void setGroupAccessMode(String projectPath, String path, boolean isOpen)
			throws EngineException
	{
		authz.checkManagerAuthorization(projectPath);
		checkIsGroupChildren(projectPath, path);
		Group group = getGroup(path);
		group.setOpen(isOpen);
		groupMan.updateGroup(path, group);
	}

	@Override
	@Transactional
	public void setGroupAuthorizationRole(String projectPath, long entityId,
			GroupAuthorizationRole role) throws EngineException
	{
		authz.checkManagerAuthorization(projectPath);
		List<String> val = new ArrayList<String>();
		val.add(role.toString());
		Attribute attr = new Attribute(
				GroupAuthorizationRoleAttributeTypeProvider.GROUP_AUTHORIZATION_ROLE,
				null, projectPath, val);
		attrHelper.addSystemAttribute(entityId, attr, true);

	}

	@Override
	@Transactional
	public List<DelegatedGroupMember> getGroupMembers(String projectPath, String path)
			throws EngineException
	{
		authz.checkManagerAuthorization(projectPath);
		checkIsGroupChildren(projectPath, path);

		List<DelegatedGroupMember> ret = new ArrayList<>();

		GroupContents contents = groupMan.getContents(path, GroupContents.MEMBERS);

		for (GroupMembership member : contents.getMembers())
		{

			long entity = member.getEntityId();
			DelegatedGroupMember entry = new DelegatedGroupMember(member.getEntityId(),
					projectPath, member.getGroup(),
					getGroupAuthRoleAttr(entity, projectPath),
					getAttributeFromMeta(entity, projectPath,
							EntityNameMetadataProvider.NAME),
					getAttributeFromMeta(entity, projectPath,
							ContactEmailMetadataProvider.NAME),
					getProjectMemberAttributes(entity, projectPath));
			ret.add(entry);
		}
		return ret;
	}

	private List<Attribute> getProjectMemberAttributes(long entity, String projectPath)
			throws EngineException
	{
		List<Attribute> ret = new ArrayList<>();

		Map<String, String> additionalAttributeTypesForGroup = getAdditionalAttributeNamesForProject(
				projectPath);

		for (String attr : additionalAttributeTypesForGroup.keySet())
		{
			Optional<Attribute> oattr = getAttribute(entity, projectPath, attr);
			if (oattr.isPresent())
				ret.add(oattr.get());

		}

		return ret;

	}

	private Optional<Attribute> getAttribute(long entityId, String path, String attribute)
			throws EngineException
	{
		Collection<AttributeExt> attributes = attrMan
				.getAttributes(new EntityParam(entityId), path, attribute);

		if (!attributes.isEmpty())
		{
			return Optional.ofNullable(attributes.iterator().next());
		}
		return Optional.empty();
	}

	private Optional<String> getAttributeValue(long entityId, String path, String attribute)
			throws EngineException
	{

		Optional<Attribute> attr = getAttribute(entityId, path, attribute);

		if (attr.isPresent())
		{
			if (!attr.get().getValues().isEmpty())
			{
				return Optional.ofNullable(
						attr.get().getValues().iterator().next());
			}
		}
		return Optional.empty();
	}

	private GroupAuthorizationRole getGroupAuthRoleAttr(long entityId, String path)
			throws EngineException
	{
		Optional<String> attrVal = getAttributeValue(entityId, path,
				GroupAuthorizationRoleAttributeTypeProvider.GROUP_AUTHORIZATION_ROLE);

		if (attrVal.isPresent())
			return GroupAuthorizationRole.valueOf(attrVal.get());

		return GroupAuthorizationRole.regular;

	}

	private String getAttributeFromMeta(long entityId, String path, String metadata)
			throws EngineException
	{

		AttributeType attrType = attrHelper.getAttributeTypeWithSingeltonMetadata(metadata);
		if (attrType == null)
			return null;

		Optional<String> value = getAttributeValue(entityId, path, attrType.getName());

		AttributeValueSyntax<?> syntax = atHelper
				.getUnconfiguredSyntaxForAttributeName(attrType.getName());
		if (syntax.isEmailVerifiable() && value.isPresent())
		{
			VerifiableEmail email = (VerifiableEmail) syntax
					.convertFromString(value.get());
			value = Optional.of(email.getValue());
		}

		return value.isPresent() ? value.get() : null;

	}

	private void checkIsGroupChildren(String projectPath, String childPath)
			throws IllegalArgumentException
	{
		if (!Group.isChildOrSame(childPath, projectPath))
			throw new IllegalArgumentException("Group " + childPath
					+ " is not child of main project group " + projectPath);
	}

	@Override
	@Transactional
	public List<Group> getProjectsForEntity(long entityId) throws EngineException
	{

		GroupStructuralData bulkData = bulkQueryService.getBulkStructuralData("/");
		Map<String, GroupContents> groupAndSubgroups = bulkQueryService
				.getGroupAndSubgroups(bulkData);

		List<Group> projects = new ArrayList<>();

		for (String group : groupAndSubgroups.keySet())
		{
			Group gr = groupAndSubgroups.get(group).getGroup();
			if (gr.getDelegationConfiguration().isEnabled())
			{
				Optional<String> val = getAttributeValue(entityId, gr.getName(),
						GroupAuthorizationRoleAttributeTypeProvider.GROUP_AUTHORIZATION_ROLE);
				if (val.isPresent() && val.get()
						.equals(GroupAuthorizationRole.manager.toString()))
				{
					projects.add(gr);

				}
			}

		}
		return projects;
	}

	@Override
	@Transactional
	public void addMemberToGroup(String projectPath, String groupPath, long entityId)
			throws EngineException
	{
		authz.checkManagerAuthorization(projectPath);
		checkIsGroupChildren(projectPath, groupPath);
		final Deque<String> notMember = getMissingEntityGroups(groupPath, entityId);
		addToGroupRecursive(notMember, entityId);

	}

	@Override
	@Transactional
	public void removeMemberFromGroup(String projectPath, String groupPath, long entityId)
			throws EngineException
	{
		authz.checkManagerAuthorization(projectPath);
		checkIsGroupChildren(projectPath, groupPath);
		groupMan.removeMember(groupPath, new EntityParam(entityId));
	}

	private Deque<String> getMissingEntityGroups(String finalGroup, long entityId)
			throws EngineException
	{
		EntityParam entityParam = new EntityParam(entityId);
		Collection<String> existingGroups;
		existingGroups = identitiesMan.getGroups(entityParam).keySet();
		final Deque<String> notMember = Group.getMissingGroups(finalGroup, existingGroups);
		return notMember;
	}

	private void addToGroupRecursive(final Deque<String> notMember, long entity)
			throws EngineException
	{
		if (notMember.isEmpty())
			return;
		String current = notMember.pollLast();
		groupMan.addMemberFromParent(current, new EntityParam(entity));
		addToGroupRecursive(notMember, entity);
	}

}
