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
 * and has a given attribute in it receives the specified 'assigned' attribute. If the condition attribute
 * has some values defined, then all of them must be present for the subject in the parent group.
 * @author K. Benedyczak
 */
public class HasParentAttributeStatement extends AttributeStatement
{
	public static final String NAME = "hasParentgroupAttribute";

	public HasParentAttributeStatement()
	{
	}
	
	public HasParentAttributeStatement(Attribute<?> assignedAttribute,
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
		return "Everybody who has a condition attribute in the parent group," +
			" receives the specified 'assigned' attribute. If the condition " +
			"attribute has some values defined, then also those values must be present.";
	}

	@Override
	public void validate(String owningGroup) throws IllegalAttributeValueException
	{
		super.validate(owningGroup);
		Group group = new Group(owningGroup);
		if (assignedAttribute == null)
			throw new IllegalAttributeValueException("The attribute statement " + NAME +
					" must have the assigned attribute set");
		if (conditionAttribute == null)
			throw new IllegalAttributeValueException("The attribute statement hasParentgroupAttribute " +
					"condition must have the attribute parameter set");
		if (group.isTopLevel())
			throw new IllegalAttributeValueException("The attribute statement hasParentgroupAttribute " +
					"condition can not be set in the root group");
		String parent = group.getParentPath();
		if (!parent.equals(conditionAttribute.getGroupPath()))
			throw new IllegalAttributeValueException("The attribute statement hasParentgroupAttribute " +
					"condition must have the attribute parameter in the parent group: " + parent);
	}
	
	@Override
	public Direction getDirection()
	{
		return Direction.upwards;
	}
	
	@Override
	public Attribute<?> evaluateCondition(Map<String, Map<String, AttributeExt<?>>> directedAttributesByGroup,
			Set<String> allGroups)
	{
		if (hasAttributeAndValInGroup(conditionAttribute, directedAttributesByGroup))
			return assignedAttribute;
		return null;
	}
}
