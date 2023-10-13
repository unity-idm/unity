/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

public enum VaadinClassNames
{
	DIALOG_CONFIRM("u-dialog-confirm"),
	SUBMIT_BUTTON("submit-button"),
	SMALL_GAP("small-gap"),
	EMPTY_DETAILS_ICON("empty-details-icon");

	private final String name;

	VaadinClassNames(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}
}
