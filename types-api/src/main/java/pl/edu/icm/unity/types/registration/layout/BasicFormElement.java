/*
 * Copyright (c) 2016 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration.layout;

import com.fasterxml.jackson.annotation.JsonCreator;

import pl.edu.icm.unity.MessageSource;

/**
 * Used in {@link FormLayout} to represent a form element being placed - for 
 * positioning of simple elements which has only one instance (e.g. comments or captcha).
 * 
 * @author Krzysztof Benedyczak
 */
public class BasicFormElement extends FormElement
{
	public BasicFormElement(FormLayoutElement type)
	{
		super(type, true);
	}
	
	@JsonCreator
	private BasicFormElement()
	{
		super(null, true);
	}
	
	@Override
	public String toString()
	{
		return "Parameter " + getType();
	}

	@Override
	public String toString(MessageSource msg)
	{
		return toString();
	}
}
