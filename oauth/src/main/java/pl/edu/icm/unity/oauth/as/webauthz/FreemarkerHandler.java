/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.idpcommon.FreemarkerHandlerBase;

/**
 * As we are not in the servlet environment, we need a thin Freemarker wrapper.
 * @author K. Benedyczak
 */
@Component
public class FreemarkerHandler extends FreemarkerHandlerBase
{
	public FreemarkerHandler()
	{
		super(FreemarkerHandler.class, "/pl/edu/icm/unity/oauth/freemarker");
	}
}
