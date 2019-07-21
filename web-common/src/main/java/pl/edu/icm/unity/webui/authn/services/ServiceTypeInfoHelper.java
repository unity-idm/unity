/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.services;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

/**
 * 
 * @author P.Piernik
 *
 */
public class ServiceTypeInfoHelper
{
	public static String getBinding(UnityMessageSource msg, String binding)
	{
		try
		{

			return msg.getMessageUnsafe("Binding." + binding);
		} catch (Exception e)
		{
			return binding;
		}
	}

	public static String getType(UnityMessageSource msg, String type)
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
