/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

public enum CSSVars
{
	TEXT_FIELD_BIG("var(--vaadin-text-field-big)"),
	RICH_FIELD_BIG("var(--vaadin-rich-field-big)"),
	TEXT_FIELD_MEDIUM("var(--vaadin-text-field-medium)"),
	BIG_MARGIN("var(--big-margin)"),
	MEDIUM_MARGIN("var(--medium-margin)"),
	SMALL_MARGIN("var(--small-margin)"),
	BASE_MARGIN("var(--base-margin)");
	
	final String var;
	
	CSSVars(String var)
	{
		this.var = var;
	}
	
	public String value()
	{
		return var;
	}
}
