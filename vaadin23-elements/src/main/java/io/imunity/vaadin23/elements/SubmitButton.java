/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.elements;


import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

import java.util.function.Function;

public class SubmitButton extends Button
{
	private ShortcutRegistration enterShortcut;
	
	public SubmitButton(Function<String, String> messageGetter)
	{
		super(messageGetter.apply("OK"));
		addClassName(Vaadin23ClassNames.SUBMIT_BUTTON.getName());
		addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		addEnterShortcut();	}
	
	public void addEnterShortcut()
	{
		enterShortcut = addClickShortcut(Key.ENTER);
	}
	
	public void removeEnterShortcut()
	{
		if (enterShortcut != null)
		{
			enterShortcut.remove();
		}
	}
}
