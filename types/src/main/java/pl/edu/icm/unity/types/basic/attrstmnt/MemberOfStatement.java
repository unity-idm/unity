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

/**
 * Assigns the given attribute to every member of a specified group.
 * 
 * @author K. Benedyczak
 */
public class MemberOfStatement extends AttributeStatement
{
	public static final String NAME = "memberOf";
	
	public MemberOfStatement()
	{}
	
	public MemberOfStatement(Attribute<?> assignedAttribute,
			String conditionGroup,
			ConflictResolution conflictResolution)
	{
		this.assignedAttribute = assignedAttribute;
		this.conflictResolution = conflictResolution;
		this.conditionGroup = conditionGroup;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Assigns the given attribute to every member of a specified condition group.";
	}

	@Override
	public void validate(String owningGroup) throws IllegalAttributeValueException
	{
		super.validate(owningGroup);
		if (assignedAttribute == null)
			throw new IllegalAttributeValueException("The attribute statement " + NAME +
					" must have the assigned attribute set");
		if (conditionGroup == null)
			throw new IllegalAttributeValueException("The attribute statement memberOf " +
					"condition must have the group parameter set");
	}

	@Override
	public Direction getDirection()
	{
		return Direction.undirected;
	}

	@Override
	public Attribute<?> evaluateCondition(
			Map<String, Map<String, AttributeExt<?>>> directedAttributesByGroup,
			Set<String> allGroups)
	{
		if (allGroups.contains(conditionGroup)) 
			return assignedAttribute;
		return null;
	}
}
