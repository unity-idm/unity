/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.Collections;
import java.util.List;

import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.types.InitializationValidator;

/**
 * Represents an attribute instance.
 * Attribute has group where it is valid (or valid and defined depending on context),
 * its syntax, visibility and list of values.
 * @author K. Benedyczak
 */
public class Attribute<T> implements InitializationValidator
{
	private AttributeValueSyntax<T> attributeSyntax;
	private String name;
	private String groupPath;
	private AttributeVisibility visibility;
	private List<T> values = Collections.emptyList();
	
	public Attribute(String name, AttributeValueSyntax<T> attributeSyntax, String groupPath, AttributeVisibility visibility,
			List<T> values)
	{
		this.attributeSyntax = attributeSyntax;
		this.name = name;
		this.groupPath = groupPath;
		this.visibility = visibility;
		setValues(values);
	}
	
	public Attribute()
	{
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
	public void setAttributeSyntax(AttributeValueSyntax<T> attributeSyntax)
	{
		this.attributeSyntax = attributeSyntax;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public void setGroupPath(String groupPath)
	{
		this.groupPath = groupPath;
	}
	public void setVisibility(AttributeVisibility visibility)
	{
		this.visibility = visibility;
	}
	public void setValues(List<T> values)
	{
		if (values == null)
			this.values = Collections.emptyList();
		else
			this.values = values;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append(": [");
		int size = values.size();
		for (int i=0; i<size; i++)
		{
			sb.append(values.get(i).toString());
			if (i<size-1)
				sb.append(", ");
		}
		sb.append("]");
		return sb.toString();
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((attributeSyntax == null) ? 0 : attributeSyntax.getValueSyntaxId().hashCode());
		result = prime * result + ((groupPath == null) ? 0 : groupPath.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		for (Object v: values)
			result = prime * result + ((v == null) ? 0 : attributeSyntax.hashCode(v));
		result = prime * result + ((visibility == null) ? 0 : visibility.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Attribute))
			return false;
		Attribute<?> other = (Attribute<?>) obj;
		
		if (attributeSyntax == null || other.attributeSyntax == null)
		{
			if (attributeSyntax != other.attributeSyntax)
				return false;
		} else if (!attributeSyntax.getValueSyntaxId().equals(other.attributeSyntax.getValueSyntaxId()))
			return false;
		
		if (groupPath == null)
		{
			if (other.groupPath != null)
				return false;
		} else if (!groupPath.equals(other.groupPath))
			return false;
		if (name == null)
		{
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;

		if (values.size() != other.values.size())
			return false;
		for (int i=0; i<values.size(); i++)
			if (!attributeSyntax.areEqual(values.get(i), other.values.get(i)))
				return false;
		
		if (visibility != other.visibility)
			return false;
		return true;
	}

	@Override
	public void validateInitialization() throws IllegalAttributeValueException
	{
		if (attributeSyntax == null)
			throw new IllegalAttributeValueException("Atribute value type must be set");
		if (name == null)
			throw new IllegalAttributeValueException("Atribute name must be set");
		if (groupPath == null)
			throw new IllegalAttributeValueException("Atribute group must be set");
		if (visibility == null)
			throw new IllegalAttributeValueException("Atribute visibility must be set");
	}
}
