/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.upman.common;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Should be use as default upMan exception which will be shown to the user when some of engine operations fails. 
 * 
 * @author P.Piernik
 *
 */
public class ServerFaultException extends ControllerException
{
	public ServerFaultException(UnityMessageSource msg)
	{
		super(msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"), null);
	}
}
