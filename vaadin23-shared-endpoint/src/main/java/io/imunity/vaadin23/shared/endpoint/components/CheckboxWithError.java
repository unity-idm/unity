/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.shared.endpoint.components;

import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.dom.Style;

public class CheckboxWithError extends CustomField<Boolean> implements HasStyle
{

	private final Checkbox checkbox;

	public CheckboxWithError() {
		checkbox = new Checkbox();
		add(checkbox);
	}

	public CheckboxWithError(String labelText) {
		checkbox = new Checkbox(labelText);
	}

	@Override
	protected Boolean generateModelValue() {
		return checkbox.getValue();
	}

	@Override
	protected void setPresentationValue(Boolean newPresentationValue) {
		checkbox.setValue(newPresentationValue);
	}

	@Override
	public void addClassNames(String... classNames) {
		checkbox.addClassNames(classNames);
	}

	@Override
	public Style getStyle() {
		return checkbox.getStyle();
	}

	public void setLabel(String newLabelText) {
		// super.setLabel(newLabelText);
		checkbox.setLabel(newLabelText);
	}

	@Override
	public void setEnabled(boolean enabled) {
		checkbox.setEnabled(enabled);
	}

	@Override
	public boolean isEnabled() {
		return checkbox.isEnabled();
	}

}