/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.credentials.password;


import com.vaadin.flow.component.textfield.PasswordField;
import io.imunity.vaadin.elements.ComponentWithLabel;

public class PasswordFieldWithContextLabel extends PasswordField
{
	private final boolean showLabelInline;

	public PasswordFieldWithContextLabel(boolean showLabelInline)
	{
		this.showLabelInline = showLabelInline;
		getElement().executeJs("Array.from(document.body.getElementsByTagName('vaadin-password-field-button'))" +
				".forEach(function (element) {element.setAttribute('tabindex', '-1')});");
	}

	@Override
	public void setLabel(String label)
	{
		String normalizedLabel = ComponentWithLabel.normalizeLabel(label);
		if (showLabelInline)
			setPlaceholder(normalizedLabel);
		else
			super.setLabel(normalizedLabel + ":");
	}
}
