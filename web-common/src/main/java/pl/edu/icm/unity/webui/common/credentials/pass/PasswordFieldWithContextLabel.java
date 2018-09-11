/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
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
		super();
		this.showLabelInline = showLabelInline;
	}

	@Override
	public void setLabel(String label)
	{
		String normalizedLabel = normalizeLabel(label);
		if (showLabelInline)
			setPlaceholder(normalizedLabel);
		else
			setCaption(normalizedLabel + ":");
	}
}
