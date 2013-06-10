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
 * Everybody who is a member of a parent group (what nearly always means: everybody)
 * and has a given attribute in it receives the same, copied condition attribute. If the condition attribute
 * has some values defined, then all of them must be present for the subject in the parent group. However all
 * always all values are copied.
 * @author K. Benedyczak
 */
public class CopyParentAttributeStatement extends AttributeStatement
{
	public static final String NAME = "copyParentGroupAttribute";

	public CopyParentAttributeStatement()
	{
	}
	
	public CopyParentAttributeStatement(Attribute<?> conditionAttribute,
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
		return "Everybody who has a condition attribute in the parent group, receives the same, " +
			"copied condition attribute the statement's group. If the condition " +
			"attribute has some values defined, then also those values must be present, " +
			"however always all values are copied.";
	}

	@Override
	public void validate(String owningGroup) throws IllegalAttributeValueException
	{
		super.validate(owningGroup);
		Group group = new Group(owningGroup);
		if (conditionAttribute == null)
			throw new IllegalAttributeValueException("The attribute statement copyParentgroupAttribute " +
					"condition must have the attribute parameter set");
		if (group.isTopLevel())
			throw new IllegalAttributeValueException("The attribute statement copyParentgroupAttribute " +
					"condition can not be set in the root group");
		String parent = group.getParentPath();
		if (!parent.equals(conditionAttribute.getGroupPath()))
			throw new IllegalAttributeValueException("The attribute statement copyParentgroupAttribute " +
					"condition must have the attribute parameter in the parent group: " + parent);
	}
	
	@Override
	public Direction getDirection()
	{
		return Direction.upwards;
	}
	
	@Override
	public String getAssignedAttributeName()
	{
		return conditionAttribute.getName();
	}
	
	@Override
	public Attribute<?> evaluateCondition(Map<String, Map<String, AttributeExt<?>>> directedAttributesByGroup,
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
