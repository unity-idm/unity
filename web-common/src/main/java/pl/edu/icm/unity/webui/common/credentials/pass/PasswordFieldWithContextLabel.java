/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.credentials.pass;

import com.vaadin.ui.PasswordField;

import pl.edu.icm.unity.webui.common.ComponentWithLabel;

/**
 * Implements contextual label on top of {@link PasswordField}.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class PasswordFieldWithContextLabel extends PasswordField implements ComponentWithLabel
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
			setCaption(normalizedLabel + ":");
	}
}
