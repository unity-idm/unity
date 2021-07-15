/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.wellknown;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.endpoint.ServletProvider;

/**
 * Provides servlet with public access to user's content stored in attributes,
 * of those syntaxes that provides {@link AttributeValueSyntax#publicExposureSpec()}
 * implementation.
 */
public interface AttributesContentPublicServletProvider extends ServletProvider
{
	public static final String SERVLET_PATH = "/content";
}
