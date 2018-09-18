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
