/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.elements;

public enum Vaadin23ClassNames
{
	DIALOG_CONFIRM("u-dialog-confirm"),
	SUBMIT_BUTTON("submit-button");

	private final String name;

	Vaadin23ClassNames(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}
}
