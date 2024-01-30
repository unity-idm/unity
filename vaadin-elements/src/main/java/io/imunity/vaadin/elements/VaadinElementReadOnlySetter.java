/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements;

public class VaadinElementReadOnlySetter
{
	public static void setReadOnly(com.vaadin.flow.dom.Element element, boolean readOnly)
	{
		element.setProperty("readonly", readOnly);
	}
}
