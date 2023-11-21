/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.Locale;
import java.util.Map;

import static io.imunity.vaadin.elements.CssClassNames.SMALL_GAP;

public class LocalizedSpan extends VerticalLayout
{
	public LocalizedSpan(Map<Locale, String> values)
	{
		values.forEach((key, value) -> add(getLayout(key, value)));
	}

	private HorizontalLayout getLayout(Locale key, String value)
	{
		HorizontalLayout layout = new HorizontalLayout(new FlagIcon(key.getLanguage()), new Span(value));
		layout.setClassName(SMALL_GAP.getName());
		return layout;
	}
}
