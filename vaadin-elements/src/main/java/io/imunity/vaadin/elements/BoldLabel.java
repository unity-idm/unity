/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.html.Span;

public class BoldLabel extends Span
{
	public BoldLabel(String text)
	{
		super(text);
		getStyle().set("font-weight", "bold");
	}
}
