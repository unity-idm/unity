/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
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
		super(FormLayout.SEPARATOR, false);
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
