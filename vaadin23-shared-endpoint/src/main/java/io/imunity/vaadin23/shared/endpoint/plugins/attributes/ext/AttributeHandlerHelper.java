/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.attributes.ext;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import io.imunity.vaadin23.shared.endpoint.plugins.attributes.AttributeViewerContext;

public class AttributeHandlerHelper
{
	public static Component getRepresentation(String value, AttributeViewerContext context)
	{
		String nvalue;

		if (context.getMaxTextSize() != null && value.length() > context.getMaxTextSize())
		{
			nvalue = value.substring(0, context.getMaxTextSize()) + " ...";
		} else
		{
			nvalue = value;
		}
		Component component;
		if (!context.isShowAsLabel())
		{
			int lines = getLineBreaks(nvalue);
			if (lines > 1)
			{
				TextArea textArea = new TextArea();
				textArea.setValue(nvalue);
				textArea.setReadOnly(true);
				component = textArea;
			}
			else
			{
				TextField textField = new TextField();
				textField.setValue(nvalue);
				textField.setReadOnly(true);
				component = textField;
			}

			if (context.isCustomWidth())
			{

				if (context.getCustomWidth() > 0)
				{
					component.getElement().getStyle().set("width", context.getCustomWidth() + context.getCustomWidthUnit().getSymbol());
				} else
				{
					component.getElement().getStyle().set("width", "unset");
				}
			}
			if (context.isCustomHeight())
			{
				if (context.getCustomHeight() > 0)
				{
					component.getElement().getStyle().set("height", context.getCustomHeight() + context.getCustomHeightUnit().getSymbol());
				}

				else
				{
					component.getElement().getStyle().set("height", "unset");
				}
			}

		} else
		{
			component = new Label(nvalue);
		}
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
		ret.add(info);
		return ret;
	}
}