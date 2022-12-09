/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.attributes;

import com.vaadin.flow.component.Component;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;

public interface AttributeSyntaxEditor<T>
{
	Component getEditor();
	
	AttributeValueSyntax<T> getCurrentValue() throws IllegalAttributeTypeException;
}
