/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.io.Serializable;

import org.mvel2.MVEL;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;


/**
 * Attribute statement allows for generating dynamic attributes. Attribute statements are
 * assigned to a group for its members. 
 * <p>
 * Attribute statement is evaluated using a MVEL context. The context is composed of several fixed 
 * variables as identities, groups and regular attributes (in the statements's group) of an entity. Additionally 
 * the statement can be configured to add to the context attributes of another group: either child or parent. 
 * <p>
 * Statement contains a condition. The attribute is assigned only if the condition evaluates to true.
 * <p>
 * Statement can assign attribute in two alternative ways: either attribute name is provided and 
 * an MVEL expression is used to produce values or a fixed attribute is given. The later variant is 
 * quite static but allows for editing assigned attribute with the full power of Unity attribute editors in the UI.
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
	
	public enum Direction
	{
		upwards,
		
		downwards,
		
		undirected
	}

	private String condition;
	private Serializable compiledCondition;
	private String extraAttributesGroup;
	private ConflictResolution conflictResolution;
	
	private Attribute fixedAttribute;
	private String dynamicAttributeType;
	private String dynamicAttributeExpression;
	private Serializable compiledDynamicAttributeExpression;
	
	
	/**
	 * Creates a statement assigning a fixed attribute
	 * @param condition
	 * @param visibility
	 * @param extraAttributesGroup
	 * @param conflictResolution
	 * @param fixedAttribute
	 */
	public AttributeStatement(String condition, 
			String extraAttributesGroup, ConflictResolution conflictResolution,
			Attribute fixedAttribute)
	{
		this.condition = condition;
		this.extraAttributesGroup = extraAttributesGroup;
		this.conflictResolution = conflictResolution;
		this.fixedAttribute = fixedAttribute;
	}

	
	/**
	 * Creates a statement assigning a dynamic attribute
	 * @param condition
	 * @param visibility
	 * @param extraAttributesGroup
	 * @param conflictResolution
	 * @param dynamicAttributeType
	 * @param dynamicAttributeExpression
	 */
	public AttributeStatement(String condition, String extraAttributesGroup, 
			ConflictResolution conflictResolution, 
			String dynamicAttributeType, String dynamicAttributeExpression)
	{
		this.condition = condition;
		this.extraAttributesGroup = extraAttributesGroup;
		this.conflictResolution = conflictResolution;
		this.dynamicAttributeType = dynamicAttributeType;
		this.dynamicAttributeExpression = dynamicAttributeExpression;
	}

	public AttributeStatement()
	{
	}

	@JsonCreator
	public AttributeStatement(ObjectNode src)
	{
		fromJson(src);
	}

	/**
	 * Creates a simple statement that assigns a given attribute to everybody
	 * @param toAssign
	 * @return
	 */
	public static AttributeStatement getFixedEverybodyStatement(Attribute toAssign)
	{
		return getFixedStatement(toAssign, null, "true");
	}

	/**
	 * Creates a statement with a given condition and assigning a fixed attribute and conflict resolution
	 * set to skip.
	 *  
	 * @param toAssign
	 * @param condition
	 * @return
	 */
	public static AttributeStatement getFixedStatement(Attribute toAssign, String extraGroup, String condition)
	{
		AttributeStatement ret = new AttributeStatement();
		ret.setCondition(condition);
		ret.setConflictResolution(ConflictResolution.skip);
		ret.setFixedAttribute(toAssign);
		return new AttributeStatement(condition, 
				extraGroup, 
				ConflictResolution.skip, 
				toAssign);
	}	
	
	public String getCondition()
	{
		return condition;
	}
	public String getExtraAttributesGroup()
	{
		return extraAttributesGroup;
	}
	public void setCondition(String condition)
	{
		this.condition = condition;
		this.compiledCondition = MVEL.compileExpression(condition);
	}
	public void setExtraAttributesGroup(String extraAttributesGroup)
	{
		this.extraAttributesGroup = extraAttributesGroup;
	}
	public ConflictResolution getConflictResolution()
	{
		return conflictResolution;
	}
	public void setConflictResolution(ConflictResolution conflictResolution)
	{
		this.conflictResolution = conflictResolution;
	}
	public Attribute getFixedAttribute()
	{
		return fixedAttribute;
	}
	public void setFixedAttribute(Attribute fixedAttribute)
	{
		this.fixedAttribute = fixedAttribute;
	}
	public String getDynamicAttributeType()
	{
		return dynamicAttributeType;
	}
	public void setDynamicAttributeType(String dynamicAttributeType)
	{
		this.dynamicAttributeType = dynamicAttributeType;
	}
	public String getDynamicAttributeExpression()
	{
		return dynamicAttributeExpression;
	}
	public void setDynamicAttributeExpression(String dynamicAttributeExpression)
	{
		this.dynamicAttributeExpression = dynamicAttributeExpression;
		this.compiledDynamicAttributeExpression = MVEL.compileExpression(dynamicAttributeExpression);
	}
	
	
	public boolean dynamicAttributeMode()
	{
		return fixedAttribute == null;
	}
	
	/**
	 * Checks if the statement can be evaluated in course of recursive evaluation of statements, without 
	 * procuring (infinite) cycles.
	 * <p>
	 * This is done as follows: statement can be always evaluated if it doen't depend on attributes in other groups.
	 * If it does it is checked whether a dependency group is in the provided direction of the current evaluation.
	 * @param direction
	 * @param groupOfStatement
	 * @return
	 */
	public boolean isSuitableForDirectedEvaluation(Direction direction, String groupOfStatement)
	{
		if (extraAttributesGroup == null)
			return true;
		if (groupOfStatement.equals(extraAttributesGroup))
			return true;
		
		if (direction == Direction.upwards)
			return groupOfStatement.startsWith(extraAttributesGroup);
		else if (direction == Direction.downwards)
			return extraAttributesGroup.startsWith(groupOfStatement);
		return false;
	}
	
	/**
	 * Name of the assigned attribute, regardless whether values are fixed or dynamic.
	 * @return
	 */
	public String getAssignedAttributeName()
	{
		return dynamicAttributeMode() ? dynamicAttributeType : fixedAttribute.getName();
			
	}

	public Serializable getCompiledCondition()
	{
		return compiledCondition;
	}
	public Serializable getCompiledDynamicAttributeExpression()
	{
		return compiledDynamicAttributeExpression;
	}
	
	public void validate(String owningGroup) throws IllegalAttributeValueException
	{
		if (dynamicAttributeExpression == null ^ dynamicAttributeType == null)
			throw new IllegalAttributeValueException("Both expression and attribute type "
					+ "must be set together");
		if (dynamicAttributeType == null && fixedAttribute == null)
			throw new IllegalAttributeValueException("Either dynamic or fixed attribute must be set");
		
		if (fixedAttribute != null && !fixedAttribute.getGroupPath().equals(owningGroup))
			throw new IllegalAttributeValueException("Fixed attribute's group must be equal to "
					+ "the statement's group");
		
		if (extraAttributesGroup != null && 
				!(owningGroup.startsWith(extraAttributesGroup) || 
						extraAttributesGroup.startsWith(owningGroup)))
			throw new IllegalAttributeValueException("The attribute statement's additional "
					+ "attributes group must be either child or parent of the statement's group");
		if (extraAttributesGroup != null && extraAttributesGroup.equals(owningGroup))
			throw new IllegalAttributeValueException("The attribute statement's additional "
					+ "attributes group must be different from the statement's group");
	}


	@Override
	public String toString()
	{
		return "Assign " + (dynamicAttributeMode() ? 
				(dynamicAttributeType + " = expr(" + dynamicAttributeExpression) + ")":
				fixedAttribute.toString())
				+ " if '" + condition + "' is true";
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.put("resolution", getConflictResolution().name());

		main.put("condition", getCondition());
		if (getExtraAttributesGroup() != null)
			main.put("extraGroupName", getExtraAttributesGroup());
		if (dynamicAttributeMode())
		{
			main.put("dynamicAttributeExpression", getDynamicAttributeExpression());
			main.put("dynamicAttributeName", getDynamicAttributeType());
		} else if (getFixedAttribute() != null)
		{
			ObjectNode attrJson = getFixedAttribute().toJson();
			main.set("fixedAttribute", attrJson);
		}
		return main;
	}
	
	private void fromJson(JsonNode as)
	{
		String resolution = as.get("resolution").asText();
		setConflictResolution(ConflictResolution.valueOf(resolution));

		setCondition(as.get("condition").asText());
		
		if (as.has("extraGroupName"))
			setExtraAttributesGroup(as.get("extraGroupName").asText());
		
		if (as.has("fixedAttribute"))
		{
			Attribute fixed = new Attribute((ObjectNode) as.get("fixedAttribute"));
			setFixedAttribute(fixed);
		} else if (as.has("dynamicAttributeName"))
		{
			setDynamicAttributeExpression(as.get("dynamicAttributeExpression").asText());
			String aTypeName = as.get("dynamicAttributeName").asText();
			setDynamicAttributeType(aTypeName);
		}
	}
	
	public AttributeStatement clone()
	{
		ObjectNode json = toJson();
		return new AttributeStatement(json);
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((condition == null) ? 0 : condition.hashCode());
		result = prime
				* result
				+ ((conflictResolution == null) ? 0 : conflictResolution.hashCode());
		result = prime
				* result
				+ ((dynamicAttributeExpression == null) ? 0
						: dynamicAttributeExpression.hashCode());
		result = prime
				* result
				+ ((dynamicAttributeType == null) ? 0 : dynamicAttributeType
						.hashCode());
		result = prime
				* result
				+ ((extraAttributesGroup == null) ? 0 : extraAttributesGroup
						.hashCode());
		result = prime * result
				+ ((fixedAttribute == null) ? 0 : fixedAttribute.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AttributeStatement other = (AttributeStatement) obj;
		if (condition == null)
		{
			if (other.condition != null)
				return false;
		} else if (!condition.equals(other.condition))
			return false;
		if (conflictResolution != other.conflictResolution)
			return false;
		if (dynamicAttributeExpression == null)
		{
			if (other.dynamicAttributeExpression != null)
				return false;
		} else if (!dynamicAttributeExpression.equals(other.dynamicAttributeExpression))
			return false;
		if (dynamicAttributeType == null)
		{
			if (other.dynamicAttributeType != null)
				return false;
		} else if (!dynamicAttributeType.equals(other.dynamicAttributeType))
			return false;
		if (extraAttributesGroup == null)
		{
			if (other.extraAttributesGroup != null)
				return false;
		} else if (!extraAttributesGroup.equals(other.extraAttributesGroup))
			return false;
		if (fixedAttribute == null)
		{
			if (other.fixedAttribute != null)
				return false;
		} else if (!fixedAttribute.equals(other.fixedAttribute))
			return false;
		return true;
	}
}
