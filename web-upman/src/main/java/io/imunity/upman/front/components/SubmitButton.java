/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.components;


import com.vaadin.flow.component.button.Button;
import pl.edu.icm.unity.MessageSource;

public class SubmitButton extends Button
{
	public SubmitButton(MessageSource msg)
	{
		super(msg.getMessage("OK"));
		addClassName("submit-button");
	}
}
