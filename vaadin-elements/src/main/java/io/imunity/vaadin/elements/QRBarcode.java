/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.elements;

import com.vaadin.flow.component.HasStyle;
import org.vaadin.barcodes.Barcode;

public class QRBarcode extends Barcode implements HasStyle
{
	public QRBarcode(String text, Type type, String width, String height)
	{
		super(text, type, width, height);
	}
}
