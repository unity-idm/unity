/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;

/**
 * Express a attribute statement, which is useful to automatically assign attributes
 * to entities fulfilling some rules.
 * <p>
 * Attribute is assigned only if some condition is fulfilled. If the attribute to be assigned is already
 * assigned to the entity, then conflict resolution is consulted.
 * <p>
 * This class holds the state required to express the condition and establish the assigned attribute. 
 * Depending on statement type, some of the data may not be required and then is null.
 * 
 * @author K. Benedyczak
 */
@Deprecated
public abstract class AttributeStatement
{
	public enum ConflictResolution {
		/**
		 * In case of conflict this statement will be skipped.
		 */
		skip, 
		
		/**
		 * In case of conflict result of this statement will overwrite the existing attribute.
		 */
		overwrite, 
		
		/**
		 * In case of conflict, the attribute values resulting from this statement will be added to the
		 * values of the existing attribute. Only new values are added. Values are added only if the attribute
		 * type has no upper cardinality bound. If so then the attribute statement is skipped.
		 */
		merge
	}
	
	public enum Direction
	{
		upwards,
		
		downwards,
		
		undirected
	}

	protected String conditionGroup;
	protected Attribute<?> conditionAttribute;
	protected Attribute<?> assignedAttribute;
	protected ConflictResolution conflictResolution;

	public AttributeStatement()
	{
	}

	public AttributeStatement(Attribute<?> assignedAttribute, ConflictResolution conflictResolution)
	{
		this.assignedAttribute = assignedAttribute;
		this.conflictResolution = conflictResolution;
	}
	
	/**
	 * @return the name of the attribute which is going to be assigned.
	 * This may be different from the attribute returned by the {@link #getAssignedAttribute()},
	 * as the method may return null for some conditions, and the attribute is determined other way 
	 * (e.g. basing on the conditions attribute in copy* statements). This method may also return null,
	 * if the assigned attribute is not known prior to evaluation. 
	 */
	public String getAssignedAttributeName()
	{
		if (assignedAttribute != null)
			return assignedAttribute.getName();
		return null;
	}
	
	public Attribute<?> getAssignedAttribute()
	{
		return assignedAttribute;
	}
	public void setAssignedAttribute(Attribute<?> assignedAttribute)
	{
		this.assignedAttribute = assignedAttribute;
	}
	public ConflictResolution getConflictResolution()
	{
		return conflictResolution;
	}
	public void setConflictResolution(ConflictResolution conflictResolution)
	{
		this.conflictResolution = conflictResolution;
	}

	public String getConditionGroup()
	{
		return conditionGroup;
	}

	public void setConditionGroup(String conditionGroup)
	{
		this.conditionGroup = conditionGroup;
	}

	public Attribute<?> getConditionAttribute()
	{
		return conditionAttribute;
	}

	public void setConditionAttribute(Attribute<?> conditionAttribute)
	{
		this.conditionAttribute = conditionAttribute;
	}

	public abstract String getName();

	public abstract String getDescription();

	public abstract Direction getDirection();
	
	public abstract Attribute<?> evaluateCondition(Map<String, Map<String, AttributeExt<?>>> directedAttributesByGroup,
			Set<String> allGroups);

	/**
	 * Useful for implementations.
	 * @param attributeToSearch
	 * @param allAttributesByGroup
	 * @return
	 */
	protected boolean hasAttributeAndValInGroup(Attribute<?> attributeToSearch, 
			Map<String, Map<String, AttributeExt<?>>> allAttributesByGroup)
	{
		Map<String, AttributeExt<?>> attributesInGroup = allAttributesByGroup.get(attributeToSearch.getGroupPath());
		if (attributesInGroup == null)
			return false;
		AttributeExt<?> attribute = attributesInGroup.get(attributeToSearch.getName());
		if (attribute == null)
			return false;
		@SuppressWarnings("unchecked")
		AttributeValueSyntax<Object> syntax = (AttributeValueSyntax<Object>) attribute.getAttributeSyntax();
		for (Object val: attributeToSearch.getValues())
		{
			boolean found = false;
			for (Object val2: attribute.getValues())
			{
				if (syntax.areEqual(val, val2))
				{
					found = true;
					break;
				}
			}
			if (!found)
				return false;
		}
		return true;
	}
	
	public void validate(String owningGroup) throws IllegalAttributeValueException
	{
		if (assignedAttribute != null)
		{
			if (!owningGroup.equals(assignedAttribute.getGroupPath()))
				throw new IllegalAttributeValueException("Assigned attribute scope must be equal " +
						"to the statement's scope. Is: " + assignedAttribute.getGroupPath() +
						", while should be: " + owningGroup);
		}
	}
}
