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

/**
 * Used in {@link FormLayout} to represent a form element being placed - for 
 * positioning of simple elements which has only one instance (e.g. comments or captcha).
 * 
 * @author Krzysztof Benedyczak
 */
public class BasicFormElement extends FormElement
{
	public BasicFormElement(String type)
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
		return "BasicFormElement []";
	}
}
