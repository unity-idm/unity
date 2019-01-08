/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

/**
 * Wraps a given component which should be a Label or other component rendered as text (e.g. button with link style).
 * The component is wrapped so that the total height is the same as for other authN screen elements.
 * It is possible to control alignment of the wrapped element inside.
 * 
 * @author K. Benedyczak
 */
public class AuthNGridTextWrapper extends CustomComponent
{
	public AuthNGridTextWrapper(Component toWrap, Alignment alignment)
	{
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setMargin(false);
		wrapper.setSpacing(false);
		wrapper.addStyleName("u-authnOptionTextElement");
		wrapper.addComponent(toWrap);
		if (alignment.isMiddle())
			wrapper.addStyleName("u-authnOptionTextCenteredElement");
		wrapper.setComponentAlignment(toWrap, alignment);
		setCompositionRoot(wrapper);
	}
}
