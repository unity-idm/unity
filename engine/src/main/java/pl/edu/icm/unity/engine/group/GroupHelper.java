/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.group;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupMembership;

/**
 * Shared group-related utility methods
 * @author K. Benedyczak
 */
@Component
public class GroupHelper
{
	private MembershipDAO membershipDAO;
	private EntityResolver entityResolver;
	private AttributeTypeDAO attributeTypeDAO;
	private AttributesHelper attributesHelper;
	private GroupDAO groupDAO;
	
	@Autowired
	public GroupHelper(MembershipDAO membershipDAO, EntityResolver entityResolver,
			AttributeTypeDAO attributeTypeDAO, AttributesHelper attributesHelper,
			GroupDAO groupDAO)
	{
		this.membershipDAO = membershipDAO;
		this.entityResolver = entityResolver;
		this.attributeTypeDAO = attributeTypeDAO;
		this.attributesHelper = attributesHelper;
		this.groupDAO = groupDAO;
	}

	/**
	 * Adds entity to the given group. The entity must be a member of the parent group
	 * (unless adding to the root group).
	 * @param path
	 * @param entity
	 * @param idp
	 * @param translationProfile
	 * @param creationTs
	 * @throws IllegalGroupValueException
	 * @throws IllegalIdentityValueException
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
	}

	/**
	 * Checks if all group's attribute statements seems correct.
	 * @param group
	 * @throws IllegalAttributeValueException
	 * @throws IllegalAttributeTypeException
	 * @throws IllegalTypeException
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
	 * @param group
	 * @param statement
	 * @throws IllegalAttributeValueException
	 * @throws IllegalAttributeTypeException
	 * @throws IllegalTypeException
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

}
