/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import com.vaadin.ui.Alignment;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.TopHeaderLight;

/**
 * Top bar of the authentication screen. Includes language selection.
 * @author K. Benedyczak
 */
public class AuthenticationTopHeader extends TopHeaderLight
{

	public AuthenticationTopHeader(String title, LocaleChoiceComponent localeChoice, MessageSource msg)
	{
		super(title, msg);
		
		addComponent(localeChoice);
		setComponentAlignment(localeChoice, Alignment.MIDDLE_RIGHT);
	}

}
