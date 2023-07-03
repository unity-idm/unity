/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.group;

import static pl.edu.icm.unity.base.audit.AuditEventTag.GROUPS;
import static pl.edu.icm.unity.base.audit.AuditEventTag.MEMBERS;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeStatement;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.attribute.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.base.audit.AuditEventAction;
import pl.edu.icm.unity.base.audit.AuditEventType;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.exceptions.IllegalTypeException;
import pl.edu.icm.unity.engine.api.group.IllegalGroupValueException;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.audit.AuditEventTrigger;
import pl.edu.icm.unity.engine.audit.AuditPublisher;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;

/**
 * Shared group-related utility methods
 * @author K. Benedyczak
 */
@Component
public class GroupHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, GroupHelper.class);
	
	private MembershipDAO membershipDAO;
	private EntityResolver entityResolver;
	private AttributeTypeDAO attributeTypeDAO;
	private AttributesHelper attributesHelper;
	private GroupDAO groupDAO;
	private AttributeDAO dbAttributes;
	private AuditPublisher audit;
	
	@Autowired
	public GroupHelper(MembershipDAO membershipDAO, EntityResolver entityResolver,
			AttributeTypeDAO attributeTypeDAO, AttributesHelper attributesHelper,
			GroupDAO groupDAO, AttributeDAO dbAttributes,
			AuditPublisher audit)
	{
		this.membershipDAO = membershipDAO;
		this.entityResolver = entityResolver;
		this.attributeTypeDAO = attributeTypeDAO;
		this.attributesHelper = attributesHelper;
		this.groupDAO = groupDAO;
		this.dbAttributes = dbAttributes;
		this.audit = audit;
	}

	/**
	 * Adds entity to the given group. The entity must be a member of the parent group
	 * (unless adding to the root group).
	 */
	public void addMemberFromParent(String path, EntityParam entity, String idp, String translationProfile,
			Date creationTs) 
				throws IllegalGroupValueException, IllegalIdentityValueException
	{
		long entityId = entityResolver.getEntityId(entity);
		Group group = new Group(path);
		if (!group.isTopLevel())
		{
			if (!membershipDAO.isMember(entityId, group.getParentPath()))
				throw new IllegalGroupValueException("Can't add to the group " + path + 
						", as the entity is not a member of its parent group");
		}
		if (membershipDAO.isMember(entityId, path))
			throw new IllegalGroupValueException("The entity is already a member of this group " + path);

		GroupMembership param = new GroupMembership(path, entityId, creationTs, translationProfile, idp);
		membershipDAO.create(param);
		audit.log(AuditEventTrigger.builder()
				.type(AuditEventType.MEMBERSHIP)
				.action(AuditEventAction.ADD)
				.subject(entityId)
				.name(group.getName())
				.tags(MEMBERS, GROUPS));
		log.info("Added entity " + entityId + " to group " + group.toString());
	}
	
	public boolean isMember(long entityId, String path)
	{
		return membershipDAO.isMember(entityId, path);
	}

	/**
	 * Checks if all group's attribute statements seems correct.
	 */
	public void validateGroupStatements(Group group) throws IllegalAttributeValueException, 
		IllegalAttributeTypeException, IllegalTypeException
	{
		AttributeStatement[] statements = group.getAttributeStatements();
		String path = group.toString();
		for (AttributeStatement statement: statements)
			validateGroupStatement(path, statement);
	}

	/**
	 * Checks if the given group's statement seems correct
	 */
	public void validateGroupStatement(String group, AttributeStatement statement) 
			throws IllegalAttributeValueException, IllegalAttributeTypeException, IllegalTypeException
	{
		statement.validate(group);
		String attributeName = statement.getAssignedAttributeName();
		AttributeType at = attributeTypeDAO.get(attributeName);
		if (at.isInstanceImmutable())
			throw new IllegalAttributeTypeException("Can not assign attribute " + at.getName() +
					" in attribute statement as the attribute type is an internal, "
					+ "system attribute.");

		Attribute fixedAttribute = statement.getFixedAttribute();
		if (statement.getConflictResolution() != ConflictResolution.merge && fixedAttribute != null)
			attributesHelper.validate(fixedAttribute, at);
		
		if (statement.getExtraAttributesGroup() != null && 
				!groupDAO.exists(statement.getExtraAttributesGroup()))
			throw new IllegalArgumentException("Group " + statement.getExtraAttributesGroup() + 
					" does not exist");
	}

	
	public List<Group> getEntityGroups(long entity)
	{
		List<Group> ret = new ArrayList<>();
		Map<String, Group> allAsMap = groupDAO.getAllAsMap();
		List<GroupMembership> entityMembership = membershipDAO.getEntityMembership(entity);
		for (GroupMembership memberhip: entityMembership)
			ret.add(allAsMap.get(memberhip.getGroup()));
		return ret;
	}
	
	
	/**
	 * Remove from group
	 */
	public void removeFromGroups(long entityId, Set<String> toRemove)
	{
		Set<String> entityMembership = membershipDAO.getEntityMembershipSimple(entityId);
		Set<String> topLevelGroups = groupDAO.getAll().stream().filter(g -> g.isTopLevel())
				.map(g -> g.toString()).collect(Collectors.toSet());
		
		Set<String> toRemoveOnlyParents = establishOnlyParentGroups(toRemove);
		toRemoveOnlyParents.removeAll(topLevelGroups);

		for (String groupToRemove : toRemoveOnlyParents)
		{
			for (String group : entityMembership)
			{
				if (Group.isChildOrSame(group, groupToRemove))
				{
					membershipDAO.deleteByKey(entityId, group);
					audit.log(AuditEventTrigger.builder()
							.type(AuditEventType.GROUP)
							.action(AuditEventAction.UPDATE)
							.name(group)
							.subject(entityId)
							.details(ImmutableMap.of("action", "remove"))
							.tags(MEMBERS, GROUPS));
					dbAttributes.deleteAttributesInGroup(entityId, group);
					log.info("Removed entity " + entityId + " from group " + group);
				}
			}
		}
	}
	
	private Set<String> establishOnlyParentGroups(Set<String> source)
	{
		Set<String> onlyParents = new HashSet<>(source);

		for (String g1 : source)
		{
			for (String g2 : source)
			{
				if (Group.isChild(g2, g1) && onlyParents.contains(g2))
				{
					onlyParents.remove(g2);
				}
			}
		}
		return onlyParents;
	}
}
