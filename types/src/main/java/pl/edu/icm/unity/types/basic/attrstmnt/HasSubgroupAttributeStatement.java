/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic.attrstmnt;

import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.Group;

/**
 * Everybody who is a member of a child group of the condition attribute 
 * and has the condition attribute in it, receives the specified 'assigned' attribute. If the condition
 * attribute has some values defined, then also those values must be present.
 * 
 * @author K. Benedyczak
 */
public class HasSubgroupAttributeStatement extends AttributeStatement
{
	public static final String NAME = "hasSubgroupAttribute";

	public HasSubgroupAttributeStatement()
	{
	}

	public HasSubgroupAttributeStatement(Attribute<?> assignedAttribute,
			Attribute<?> conditionAttribute,
			ConflictResolution conflictResolution)
	{
		this.assignedAttribute = assignedAttribute;
		this.conflictResolution = conflictResolution;
		this.conditionAttribute = conditionAttribute;
	}
	
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Everybody who is a member of a child group of the condition attribute"+ 
		 " and has the condition attribute in it, receives the specified 'assigned' attribute. If the condition " +
		 "attribute has some values defined, then also those values must be present.";
	}

	@Override
	public void validate(String owningGroup) throws IllegalAttributeValueException
	{
		super.validate(owningGroup);
		if (assignedAttribute == null)
			throw new IllegalAttributeValueException("The attribute statement " + NAME +
					" must have the assigned attribute set");
		Group group = new Group(owningGroup);
		if (conditionAttribute == null)
			throw new IllegalAttributeValueException("The attribute statement hasSubgroupAttribute " +
					"condition must have the attribute parameter set");
		String attrGroup = conditionAttribute.getGroupPath();
		if (attrGroup == null)
			throw new IllegalAttributeValueException("The attribute statement hasSubgroupAttribute " +
					"condition must have the attribute parameter with the group scope set");
		Group child = new Group(attrGroup);
		if (!child.isChild(group) || child.getPath().length != group.getPath().length+1)
			throw new IllegalAttributeValueException("The attribute statement hasSubgroupAttribute " +
					"condition must have the attribute parameter with the group scope set " +
					"to a immediate subgroup of the statement group");
	}

	@Override
	public Direction getDirection()
	{
		return Direction.downwards;
	}

	@Override
	public Attribute<?> evaluateCondition(
			Map<String, Map<String, AttributeExt<?>>> directedAttributesByGroup,
			Set<String> allGroups)
	{
		if (hasAttributeAndValInGroup(conditionAttribute, directedAttributesByGroup))
			return assignedAttribute;
		return null;
	}
}
