/*
 * Copyright (c) 2016 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration.layout;

import pl.edu.icm.unity.MessageSource;

/**
 * Represents a visual separator between sections. 
 * @author Krzysztof Benedyczak
 */
public class FormSeparatorElement extends FormElement
{
	public FormSeparatorElement()
	{
		super(FormLayoutElement.SEPARATOR, false);
	}

	@Override
	public String toString()
	{
		return "---------------";
	}

	@Override
	public String toString(MessageSource msg)
	{
		return toString();
	}
}
