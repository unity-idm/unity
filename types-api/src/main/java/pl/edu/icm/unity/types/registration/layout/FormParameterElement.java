/*
 * Copyright (c) 2016 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration.layout;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;

import pl.edu.icm.unity.MessageSource;

/**
 * Represents one of the variable form elements, which can have multiple 
 * occurrences: attributes, identities, groups and credentials.
 * 
 * @author Krzysztof Benedyczak
 */ 
public class FormParameterElement extends FormElement
{
	private int index;

	public FormParameterElement(FormLayoutElement type, int index)
	{
		super(type, true);
		this.index = index;
	}
	
	@JsonCreator
	private FormParameterElement()
	{
		super(null, true);
	}
	
	public int getIndex()
	{
		return index;
	}

	@Override
	public String toString()
	{
		return "Parameter " + getType() + " [" + index + "]";
	}

	@Override
	public String toString(MessageSource msg)
	{
		return toString();
	}

	@Override
	public boolean equals(final Object other)
	{
		if (this == other)
			return true;
		if (!(other instanceof FormParameterElement))
			return false;
		if (!super.equals(other))
			return false;
		FormParameterElement castOther = (FormParameterElement) other;
		return Objects.equals(index, castOther.index);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), index);
	}
}
