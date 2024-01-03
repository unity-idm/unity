/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.ComponentEvent;

public class AfterSubNavigationEvent extends ComponentEvent<UnityViewComponent>
{
	public AfterSubNavigationEvent(UnityViewComponent source, boolean fromClient)
	{
		super(source, fromClient);
	}
}
