/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;


import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

import java.util.function.Function;

public class SubmitButton extends Button
{
	public SubmitButton(Function<String, String> messageGetter)
	{
		super(messageGetter.apply("OK"));
		addClassName(Vaadin23ClassNames.SUBMIT_BUTTON.getName());
		addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		addClickShortcut(Key.ENTER);
	}
}
