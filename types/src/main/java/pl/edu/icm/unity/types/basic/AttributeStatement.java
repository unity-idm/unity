/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

/**
 * Express a attribute statement, which is useful to automatically assign attributes
 * to entities fulfilling some rules.
 * <p>
 * Attribute is assigned only if all conditions are fulfilled. If the attribute to be assigned is already
 * assigned to the entity, then conflict resolution is consulted.
 * @see AttributeStatementCondition 
 * @author K. Benedyczak
 */
public class AttributeStatement
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

	private AttributeStatementCondition[] conditions;
	private Attribute<?> assignedAttribute;
	private ConflictResolution conflictResolution;

	public AttributeStatement()
	{
	}

	public AttributeStatement(AttributeStatementCondition[] conditions,
			Attribute<?> assignedAttribute, ConflictResolution conflictResolution)
	{
		this.conditions = conditions;
		this.assignedAttribute = assignedAttribute;
		this.conflictResolution = conflictResolution;
	}
	
	public AttributeStatementCondition[] getConditions()
	{
		return conditions;
	}
	public void setConditions(AttributeStatementCondition[] conditions)
	{
		this.conditions = conditions;
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
}
