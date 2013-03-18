/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;


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
		 * attribute param, its scope and values are ignored)
		 */
		hasParentgroupAttribute, 
		
		/**
		 * Everybody who is a member of a parent group (except special cases - everybody) 
		 * and has an attribute in it with at least all the values which are in the attribute.
		 * (requires  attribute param, its scope is ignored)
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
}
