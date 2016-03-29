/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.types.registration.layout;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Used in {@link FormLayout} to represent a form element being placed.
 * 
 * 
 * @author Krzysztof Benedyczak
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.EXISTING_PROPERTY, property="clazz")
public abstract class FormElement
{
	private String clazz;
	private String type;
	private boolean formContentsRelated;

	public FormElement(String type, boolean formContentsRelated)
	{
		super();
		this.type = type;
		this.formContentsRelated = formContentsRelated;
		this.clazz = getClass().getName();
	}

	public String getType()
	{
		return type;
	}

	public boolean isFormContentsRelated()
	{
		return formContentsRelated;
	}

	public String getClazz()
	{
		return clazz;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
		result = prime * result + (formContentsRelated ? 1231 : 1237);
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		FormElement other = (FormElement) obj;
		if (clazz == null)
		{
			if (other.clazz != null)
				return false;
		} else if (!clazz.equals(other.clazz))
			return false;
		if (formContentsRelated != other.formContentsRelated)
			return false;
		if (type == null)
		{
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
}
