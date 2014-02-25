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
 * Assigns the given attribute to every member.
 * 
 * @author K. Benedyczak
 */
public class EverybodyStatement extends AttributeStatement
{
	public static final String NAME = "everybody";
	
	public EverybodyStatement() 
	{}
	
	public EverybodyStatement(Attribute<?> assignedAttribute, ConflictResolution conflictResolution)
	{
		this.assignedAttribute = assignedAttribute;
		this.conflictResolution = conflictResolution;
	}
	
	@Override
	public void validate(String owningGroup) throws IllegalAttributeValueException
	{
		super.validate(owningGroup);
		if (assignedAttribute == null)
			throw new IllegalAttributeValueException("The attribute statement " + NAME +
					" must have the assigned attribute set");
	}
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Assigns the given attribute to every member.";
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
		String ourGroup = assignedAttribute.getGroupPath();
		if (!allGroups.contains(ourGroup))
			return null;
		return assignedAttribute;
	}
}
