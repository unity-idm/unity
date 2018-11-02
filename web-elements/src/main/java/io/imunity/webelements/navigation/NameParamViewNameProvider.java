/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.navigation;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;


/**
 * 
 * @author P.Piernik
 *
 */
public class NameParamViewNameProvider implements ViewDisplayNameProvider
{

	@Override
	public String getDisplayName(ViewChangeEvent event)
	{		
		return event.getParameterMap().isEmpty() || !event.getParameterMap().containsKey("name")? ""
				: event.getParameterMap().get("name");
	}

}
