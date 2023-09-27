/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.dom.Element;

public class QRBarcode extends Component implements HasStyle
{
	public QRBarcode(String text, String type, String width, String height)
	{
//		super(text, type, width, height);
	}

	@Override
	public Element getElement()
	{
		return new Element("a");
	}
}
