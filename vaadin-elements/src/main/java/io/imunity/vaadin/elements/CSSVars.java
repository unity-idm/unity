/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

public enum CSSVars
{
	TEXT_FIELD_BIG("var(--unity-text-field-big)"),
	RICH_FIELD_BIG("var(--unity-rich-field-big)"),
	TEXT_FIELD_MEDIUM("var(--unity-text-field-medium)"),
	BIG_MARGIN("var(--unity-big-margin)"),
	MEDIUM_MARGIN("var(--unity-medium-margin)"),
	SMALL_MARGIN("var(--unity-small-margin)"),
	BASE_MARGIN("var(--unity-base-margin)");
	
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
