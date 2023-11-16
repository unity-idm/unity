/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

public enum VaadinClassNames
{
	DIALOG_CONFIRM("u-dialog-confirm"),
	SUBMIT_BUTTON("submit-button"),
	SMALL_GAP("u-small-gap"),
	EMPTY_DETAILS_ICON("u-empty-details-icon"),
	GRID_DETAILS_FORM_ITEM("u-grid-details-vaadin-form-item"),
	BOLD("u-bold"),
	IMMUTABLE_ATTRIBUTE("u-immutableAttribute"),
	GRID_DETAILS_FORM("u-grid-details-vaadin-form"),
	DISABLED_ICON("u-disabled-icon"),
	POINTER("u-pointer"),
	FIELD_ICON_GAP("u-field-icon-gap"),
	DROP_LAYOUT("u-drop-layout"),
	PANEL("u-panel"),
	TRUE_CONDITION_BACKGROUND("u-trueCondition-bg"),
	FALSE_CONDITION_BACKGROUND("u-falseCondition-bg"),
	ERROR_BACKGROUND("u-error-bg"),
	MONOSPACE("u-monospace"),
	BIG_VAADIN_FORM_ITEM_LABEL("u-big-vaadin-form-item"),
	MEDIUM_VAADIN_FORM_ITEM_LABEL("u-medium-vaadin-form-item"),
	LOGO_IMAGE("u-logo-image");

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
