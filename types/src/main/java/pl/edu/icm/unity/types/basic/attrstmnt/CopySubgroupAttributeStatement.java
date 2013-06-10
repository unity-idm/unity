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
public class CopySubgroupAttributeStatement extends AttributeStatement
{
	public static final String NAME = "copySubgroupAttribute";

	public CopySubgroupAttributeStatement()
	{
	}

	public CopySubgroupAttributeStatement(Attribute<?> conditionAttribute,
			ConflictResolution conflictResolution)
	{
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
		 " and has the condition attribute in it, receives the copy of the condition attribute from the subgroup. " +
		 "If the condition attribute has some values defined, then also those values must be present, however" +
		 " always all values are copied.";
	}

	@Override
	public void validate(String owningGroup) throws IllegalAttributeValueException
	{
		super.validate(owningGroup);
		Group group = new Group(owningGroup);
		if (conditionAttribute == null)
			throw new IllegalAttributeValueException("The attribute statement copySubgroupAttribute " +
					"condition must have the attribute parameter set");
		String attrGroup = conditionAttribute.getGroupPath();
		if (attrGroup == null)
			throw new IllegalAttributeValueException("The attribute statement copySubgroupAttribute " +
					"condition must have the attribute parameter with the group scope set");
		Group child = new Group(attrGroup);
		if (!child.isChild(group) || child.getPath().length != group.getPath().length+1)
			throw new IllegalAttributeValueException("The attribute statement copySubgroupAttribute " +
					"condition must have the attribute parameter with the group scope set " +
					"to a immediate subgroup of the statement group");
	}

	@Override
	public Direction getDirection()
	{
		return Direction.downwards;
	}

	@Override
	public String getAssignedAttributeName()
	{
		return conditionAttribute.getName();
	}

	@Override
	public Attribute<?> evaluateCondition(
			Map<String, Map<String, AttributeExt<?>>> directedAttributesByGroup,
			Set<String> allGroups)
	{
		if (hasAttributeAndValInGroup(conditionAttribute, directedAttributesByGroup))
		{
			Map<String, AttributeExt<?>> attributes = 
					directedAttributesByGroup.get(conditionAttribute.getGroupPath());
			return attributes.get(conditionAttribute.getName());
		}
		return null;
	}
}
