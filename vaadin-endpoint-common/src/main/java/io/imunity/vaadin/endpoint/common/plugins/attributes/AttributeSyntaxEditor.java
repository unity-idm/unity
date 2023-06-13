/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes;

import com.vaadin.flow.component.Component;

import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;

public interface AttributeSyntaxEditor<T>
{
	Component getEditor();
	
	AttributeValueSyntax<T> getCurrentValue() throws IllegalAttributeTypeException;
}
