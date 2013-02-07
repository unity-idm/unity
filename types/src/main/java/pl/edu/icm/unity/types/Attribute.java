/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import java.util.List;

/**
 * Represents an attribute instance.
 * Attribute has group where it is valid (or valid and defined depending on context),
 * its syntax, visibility and list of values.
 * @author K. Benedyczak
 */
public class Attribute<T>
{
	private AttributeValueSyntax<T> attributeSyntax;
	private String name;
	private String groupPath;
	private AttributeVisibility visibility;
	private List<T> values;
	
	public Attribute(String name, AttributeValueSyntax<T> attributeSyntax, String groupPath, AttributeVisibility visibility,
			List<T> values)
	{
		this.attributeSyntax = attributeSyntax;
		this.name = name;
		this.groupPath = groupPath;
		this.visibility = visibility;
		this.values = values;
	}
	public AttributeValueSyntax<T> getAttributeSyntax()
	{
		return attributeSyntax;
	}
	public String getGroupPath()
	{
		return groupPath;
	}
	public List<T> getValues()
	{
		return values;
	}
	public AttributeVisibility getVisibility()
	{
		return visibility;
	}
	public String getName()
	{
		return name;
	}
}
