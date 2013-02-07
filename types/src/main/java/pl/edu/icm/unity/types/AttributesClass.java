/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

/**
 * Attributes class defines a set of mandatory attributes for an entity which has this class assigned.
 * Attribute class can have a parent: all attributes from the parent are also included in this class.
 * @author K. Benedyczak
 */
public class AttributesClass
{
	private String id;
	private String parentClassId;
	private String[] attributeTypes;
	
	public AttributesClass(String id, String[] attributeTypes, String parentClassId)
	{
		this.id = id;
		this.attributeTypes = attributeTypes;
		this.parentClassId = parentClassId;
	}
	
	public String getId()
	{
		return id;
	}
	public String[] getAttributeTypes()
	{
		return attributeTypes;
	}

	public String getParentClassId()
	{
		return parentClassId;
	}
}
