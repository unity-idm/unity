/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;

/**
 * {@link FormLayout} extension with disabled margins and spacing. Spacing is still preserved as
 * the captions have minheight.
 * @author K. Benedyczak
 */
public class CompactFormLayout extends FormLayout
{

	public CompactFormLayout()
	{
		super();
		setup();
	}

	public CompactFormLayout(Component... children)
	{
		super(children);
		setup();
	}

	private void setup()
	{
		setMargin(false);
		setSpacing(false);
		addStyleName(Styles.smallFormSpacing.toString());
	}
}
