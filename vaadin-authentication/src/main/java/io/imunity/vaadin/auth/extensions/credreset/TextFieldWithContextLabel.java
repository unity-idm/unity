/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.extensions.credreset;

import com.vaadin.flow.component.textfield.TextField;
import pl.edu.icm.unity.webui.common.ComponentWithLabel;

/**
 * Depending on configuration label may be put as placeholder or as a standard caption of this textfield
 */
public class TextFieldWithContextLabel extends TextField implements ComponentWithLabel
{
	private final boolean showLabelInline;

	public TextFieldWithContextLabel(boolean showLabelInline)
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
