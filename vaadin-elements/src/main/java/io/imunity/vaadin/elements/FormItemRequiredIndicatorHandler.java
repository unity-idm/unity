/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.Component;

public class FormItemRequiredIndicatorHandler
{

	private static final String LUMO_REQUIRED_FIELD_INDICATOR_COLOR = "--lumo-required-field-indicator-color";

	public static void setInvalid(Component element, boolean invalid)
	{
		if(invalid)
			element.getParent().ifPresent(parent -> parent.getStyle().set(LUMO_REQUIRED_FIELD_INDICATOR_COLOR, "var(--lumo-error-text-color)"));
		else
			element.getParent().ifPresent(parent -> parent.getStyle().set(LUMO_REQUIRED_FIELD_INDICATOR_COLOR, "var(--lumo-primary-text-color)"));
	}
}
