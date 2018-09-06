/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.types.registration.layout;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;

import pl.edu.icm.unity.MessageSource;
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
		super(FormLayoutElement.CAPTION, false);
		this.value = value;	
	}
	
	@JsonCreator
	private FormCaptionElement()
	{
		super(FormLayoutElement.CAPTION, false);
	}
	
	public I18nString getValue()
	{
		return value;
	}

	@Override
	public String toString(MessageSource msg)
	{
		return "Caption '" + value.getValue(msg) + "'";
	}
	
	@Override
	public String toString()
	{
		return "Caption '" + value + "'";
	}

	@Override
	public boolean equals(final Object other)
	{
		if (this == other)
			return true;
		if (!(other instanceof FormCaptionElement))
			return false;
		if (!super.equals(other))
			return false;
		FormCaptionElement castOther = (FormCaptionElement) other;
		return Objects.equals(value, castOther.value);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), value);
	}
}
