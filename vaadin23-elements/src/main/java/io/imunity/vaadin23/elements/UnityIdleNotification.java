/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.elements;

import com.vaadin.componentfactory.IdleNotification;
import com.vaadin.flow.component.dependency.CssImport;


@CssImport("./styles/components/idle-notification.css")
public class UnityIdleNotification extends IdleNotification
{
	public UnityIdleNotification(int secondsBeforeNotification) throws IllegalArgumentException
	{
		super(secondsBeforeNotification);
	}
}
