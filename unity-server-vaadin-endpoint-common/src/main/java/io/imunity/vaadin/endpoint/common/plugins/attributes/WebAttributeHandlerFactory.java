/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;

public interface WebAttributeHandlerFactory
{
	String getSupportedSyntaxId();
	
	WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax);

	AttributeSyntaxEditor<?> getSyntaxEditorComponent(AttributeValueSyntax<?> syntax);
}
