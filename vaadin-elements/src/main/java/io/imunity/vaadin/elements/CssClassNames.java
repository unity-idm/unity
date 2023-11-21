/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

public enum CssClassNames
{
	DIALOG_CONFIRM("u-dialog-confirm"),
	SUBMIT_BUTTON("u-submit-button"),
	SMALL_GAP("u-small-gap"),
	EMPTY_DETAILS_ICON("u-empty-details-icon"),
	GRID_DETAILS_FORM_ITEM("u-grid-details-vaadin-form-item"),
	BOLD("u-bold"),
	UNDERLINE("u-underline"),
	IMMUTABLE_ATTRIBUTE("u-immutableAttribute"),
	GRID_DETAILS_FORM("u-grid-details-vaadin-form"),
	DISABLED_ICON("u-disabled-icon"),
	POINTER("u-pointer"),
	FIELD_ICON_GAP("u-field-icon-gap"),
	DROP_LAYOUT("u-drop-layout"),
	PANEL("u-panel"),
	NO_PADDING_TOP("u-no-padding-top"),
	TRUE_CONDITION_BACKGROUND("u-trueCondition-bg"),
	FALSE_CONDITION_BACKGROUND("u-falseCondition-bg"),
	ERROR_BACKGROUND("u-error-bg"),
	MONOSPACE("u-monospace"),
	BIG_VAADIN_FORM_ITEM_LABEL("u-big-vaadin-form-item"),
	MEDIUM_VAADIN_FORM_ITEM_LABEL("u-medium-vaadin-form-item"),
	MARGIN_VERTICAL("u-margin-vertical"),
	WARNING_ICON("u-warning-icon"),
	DETAILS_ICON("u-details-icon"),
	INDICATOR("u-indicator"),
	INVALID("u-invalid"),
	SMALL_ICON("u-small-icon"),
	REQUIRED_LABEL("u-required-label"),
	ERROR_INDICATOR("u-error-indicator"),
	LOGO_IMAGE("u-logo-image");

	private final String name;

	CssClassNames(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}
}
