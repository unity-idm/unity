/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements;

import com.vaadin.flow.component.html.Span;

public class ErrorLabel extends Span
{
	public ErrorLabel(String label)
	{
		add(label);
		addClassName("error-label");
	}
}
