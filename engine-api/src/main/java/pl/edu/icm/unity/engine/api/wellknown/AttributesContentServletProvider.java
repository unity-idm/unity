/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.wellknown;

import javax.servlet.Filter;

import org.eclipse.jetty.servlet.ServletHolder;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;

/**
 * Provides servlet with access to user's content in attributes of those
 * syntaxes that provides {@link AttributeValueSyntax#shareSpec()}
 * implementation.
 */
public interface AttributesContentServletProvider
{
	public static final String SERVLET_PATH = "/content";

	ServletHolder getServiceServlet();

	Filter getServiceFilter();
}
