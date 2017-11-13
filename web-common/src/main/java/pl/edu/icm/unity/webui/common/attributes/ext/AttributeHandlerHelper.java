/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Helper for attributes handlers
 * 
 * @author P.Piernik
 *
 */
public class AttributeHandlerHelper
{

	public static String getValueAsString(String value)
	{
		return value.toString();
	}

	public static Component getRepresentation(String value)
	{
		return new Label(getValueAsString(value));
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
