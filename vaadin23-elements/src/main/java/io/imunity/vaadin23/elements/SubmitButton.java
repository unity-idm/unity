/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.elements;


import com.vaadin.flow.component.button.Button;

import java.util.function.Function;

public class SubmitButton extends Button
{
	public SubmitButton(Function<String, String> messageGetter)
	{
		super(messageGetter.apply("OK"));
		addClassName(Vaadin23ClassNames.SUBMIT_BUTTON.getName());
	}
}
