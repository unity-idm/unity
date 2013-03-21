/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;


/**
 * Defines when attribute statement should be applied.
 * See {@link AttributeStatement}
 * @author K. Benedyczak
 */
public class AttributeStatementCondition
{
	
	public enum Type {
		/**
		 * Everybody.
		 */
		everybody,
		
		/**
		 * Everybody who is a member of the specified subgroup (requires group param)
		 */
		memberOf, 
		
		/**
		 * Everybody who is a member of an immediate subgroup and has an attribute in it (requires 
		 * attribute param, its scope specifies the group, values are ignored)
		 */
		hasSubgroupAttribute, 
		
		/**
		 * Everybody who is a member of an immediate subgroup and has an attribute in it with 
		 * at least all the values which are in the attribute. (requires 
		 * attribute param, its scope specifies the group)
		 */
		hasSubgroupAttributeValue, 
		
		/**
		 * Everybody who is a member of a parent group (except special cases - everybody) 
		 * and has an attribute in it. (requires 
		 * attribute param, its scope must be set to the parent group and values are ignored)
		 */
		hasParentgroupAttribute, 
		
		/**
		 * Everybody who is a member of a parent group (except special cases - everybody) 
		 * and has an attribute in it with at least all the values which are in the attribute.
		 * (requires attribute param, its scope must be set to the parent group)
		 */
		hasParentgroupAttributeValue
	}

	private Type type;
	private String group;
	private Attribute<?> attribute;

	public AttributeStatementCondition()
	{
	}
	
	public AttributeStatementCondition(Type type)
	{
		this.type = type;
	}
	public Type getType()
	{
		return type;
	}
	public void setType(Type type)
	{
		this.type = type;
	}
	public String getGroup()
	{
		return group;
	}
	public void setGroup(String group)
	{
		this.group = group;
	}
	public Attribute<?> getAttribute()
	{
		return attribute;
	}
	public void setAttribute(Attribute<?> attribute)
	{
		this.attribute = attribute;
	}
	
	public void validate(String owningGroup)
	{
		Group group = new Group(owningGroup);
		if (type == null)
			throw new IllegalAttributeValueException("Condition type must be set");
		switch (type)
		{
		case memberOf:
			if (this.group == null)
				throw new IllegalAttributeValueException("The attribute statement memberOf " +
						"condition must have the group parameter set");
			break;
		case everybody:
			break;
		case hasParentgroupAttribute:
		case hasParentgroupAttributeValue:
			if (attribute == null)
				throw new IllegalAttributeValueException("The attribute statement hasParentgroupAttribute* " +
						"condition must have the attribute parameter set");
			if (group.isTopLevel())
				throw new IllegalAttributeValueException("The attribute statement hasParentgroupAttribute* " +
						"condition can not be set in the root group");
			String parent = group.getParentPath();
			if (!parent.equals(attribute.getGroupPath()))
				throw new IllegalAttributeValueException("The attribute statement hasParentgroupAttribute* " +
						"condition must have the attribute parameter in the parent group: " + parent);
			break;
		case hasSubgroupAttribute:
		case hasSubgroupAttributeValue:
			if (attribute == null)
				throw new IllegalAttributeValueException("The attribute statement hasSubgroupAttribute* " +
						"condition must have the attribute parameter set");
			String attrGroup = attribute.getGroupPath();
			if (attrGroup == null)
				throw new IllegalAttributeValueException("The attribute statement hasSubgroupAttribute* " +
						"condition must have the attribute parameter with the group scope set");
			Group child = new Group(attrGroup);
			if (!child.isChild(group) || child.getPath().length != group.getPath().length+1)
				throw new IllegalAttributeValueException("The attribute statement hasSubgroupAttribute* " +
						"condition must have the attribute parameter with the group scope set " +
						"to a immediate subgroup of the statement group");
			break;
		}
	}
}
