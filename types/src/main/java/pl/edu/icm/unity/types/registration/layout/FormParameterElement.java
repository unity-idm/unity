/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.types.registration.layout;

/**
 * Represents one of the variable form elements, which can have multiple 
 * occurrences: attributes, identities, groups and credentials.
 * 
 * @author Krzysztof Benedyczak
 */ 
public class FormParameterElement extends FormElement
{
	private int index;

	public FormParameterElement(String type, int index)
	{
		super(type);
		this.index = index;
	}

	public int getIndex()
	{
		return index;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + index;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		FormParameterElement other = (FormParameterElement) obj;
		if (index != other.index)
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "FormParameterElement [index=" + index + ", type=" + getType() + "]";
	}
}
