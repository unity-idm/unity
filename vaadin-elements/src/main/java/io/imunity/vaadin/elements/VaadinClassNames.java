/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.dependency.CssImport;

@CssImport("./styles/components/common.css")
public enum VaadinClassNames
{
	DIALOG_CONFIRM("u-dialog-confirm"),
	SUBMIT_BUTTON("submit-button");

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