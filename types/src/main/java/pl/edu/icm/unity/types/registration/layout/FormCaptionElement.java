/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.types.registration.layout;

import com.fasterxml.jackson.annotation.JsonCreator;

import pl.edu.icm.unity.types.I18nString;

/**
 * represents a fixed section caption
 * @author Krzysztof Benedyczak
 */
public class FormCaptionElement extends FormElement
{
	private I18nString value;

	public FormCaptionElement(I18nString value)
	{
		super(FormLayout.CAPTION, false);
		this.value = value;	
	}
	
	@JsonCreator
	private FormCaptionElement()
	{
		super(FormLayout.CAPTION, false);
	}
	
	public I18nString getValue()
	{
		return value;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		FormCaptionElement other = (FormCaptionElement) obj;
		if (value == null)
		{
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "FormCaptionElement [value=" + value + ", type=" + getType() + "]";
	}
}
