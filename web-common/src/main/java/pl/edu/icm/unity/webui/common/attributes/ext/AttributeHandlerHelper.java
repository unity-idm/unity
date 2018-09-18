/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.webui.common.ReadOnlyArea;
import pl.edu.icm.unity.webui.common.ReadOnlyField;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;
import pl.edu.icm.unity.webui.common.attributes.TextOnlyAttributeHandler;

/**
 * Helper for attributes handlers
 * 
 * @author P.Piernik
 *
 */
public class AttributeHandlerHelper
{
	public static Component getRepresentation(String value, AttributeValueSyntax<?> syntax, AttributeViewerContext context)
	{
		Component component;
		if (isLarge(syntax))
			component = new ReadOnlyArea(value);
		else
			component = new ReadOnlyField(value);
		
		if (context.isCustomWidth())
			component.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());

		return component;
	}
	
	public static boolean isLarge(AttributeValueSyntax<?> syntax)
	{
		if (syntax instanceof StringAttributeSyntax)
		{
			StringAttributeSyntax sas = (StringAttributeSyntax) syntax;
			if (sas.getMaxLength() > TextOnlyAttributeHandler.LARGE_STRING)
				return true;
		}
		return false;
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
