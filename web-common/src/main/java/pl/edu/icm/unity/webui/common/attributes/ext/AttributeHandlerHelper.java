/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.webui.common.ReadOnlyArea;
import pl.edu.icm.unity.webui.common.ReadOnlyField;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;

/**
 * Helper for attributes handlers
 * 
 * @author P.Piernik
 *
 */
public class AttributeHandlerHelper
{
	public static Component getRepresentation(String value, AttributeViewerContext context)
	{
		Component component;
		int lines = getLineBreaks(value);
		if (lines > 1)
			component = new ReadOnlyArea(value, lines);
		else
			component = new ReadOnlyField(value);
		
		if (context.isCustomWidth())
			component.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());

		return component;
	}
	
	private static int getLineBreaks(String string)
	{
		String lineSeparator = System.getProperty("line.separator");
		return string.split(lineSeparator).length;
	}

	public static VerticalLayout getEmptyEditor()
	{
		VerticalLayout layout = new VerticalLayout();
		layout.setSpacing(false);
		layout.setMargin(false);
		return layout;
	}

	public static Component getEmptySyntaxViewer(String msg)
	{
		VerticalLayout ret = new VerticalLayout();
		ret.setSpacing(false);
		ret.setMargin(false);
		Label info = new Label(msg);
		ret.addComponent(info);
		return ret;
	}
}
