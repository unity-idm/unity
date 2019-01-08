/*
 * Copyright (c) 2016 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration.layout;

import com.fasterxml.jackson.annotation.JsonCreator;

import pl.edu.icm.unity.MessageSource;

/**
 * represents a fixed button with custom caption
 */
public class FormLocalSignupButtonElement extends FormElement
{
	@JsonCreator
	public FormLocalSignupButtonElement()
	{
		super(FormLayoutElement.LOCAL_SIGNUP, true);
	}
	
	@Override
	public String toString(MessageSource msg)
	{
		return toString();
	}
	
	@Override
	public String toString()
	{
		return "Parameter " + getType();
	}
}
