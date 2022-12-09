/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.credentials.password;


import com.vaadin.flow.component.textfield.PasswordField;
import pl.edu.icm.unity.webui.common.ComponentWithLabel;

public class PasswordFieldWithContextLabel extends PasswordField
{
	private final boolean showLabelInline;

	public PasswordFieldWithContextLabel(boolean showLabelInline)
	{
		this.showLabelInline = showLabelInline;
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
