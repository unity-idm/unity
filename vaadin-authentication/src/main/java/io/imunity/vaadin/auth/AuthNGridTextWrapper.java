/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Wraps a given component which should be a Label or other component rendered as text (e.g. button with link style).
 * The component is wrapped so that the total height is the same as for other authN screen elements.
 * It is possible to control alignment of the wrapped element inside.
 */
public class AuthNGridTextWrapper extends VerticalLayout
{
	public AuthNGridTextWrapper(Component toWrap, Alignment alignment)
	{
		setMargin(false);
		setPadding(false);
		addClassName("u-authnOptionTextElement");
		getStyle().set("height", "var(--unity-auth-component-height)");
		getStyle().set("margin", "var(--unity-auth-component-margin) 0");
		add(toWrap);
		if (alignment.equals(Alignment.CENTER))
			addClassName("u-authnOptionTextCenteredElement");
		setAlignItems(alignment);
	}
}
