/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services;

import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Convert service binding and type to more readable form
 * 
 * @author P.Piernik
 *
 */
public class ServiceTypeInfoHelper
{
	public static String getBinding(MessageSource msg, String binding)
	{
		try
		{

			return msg.getMessageUnsafe("Binding." + binding);
		} catch (Exception e)
		{
			return binding;
		}
	}

	public static String getType(MessageSource msg, String type)
	{
		try
		{

			return msg.getMessage("ServiceType." + type);
		} catch (Exception e)
		{
			return type;
		}
	}
}
