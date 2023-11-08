/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

public enum Styles
{
	dropLayout("drop-layout"),
	panel("panel");
	
	private String value;

	private Styles(String value)
	{
		this.value = value;
	}

	@Override
	public String toString()
	{
		return value;
	}
}
