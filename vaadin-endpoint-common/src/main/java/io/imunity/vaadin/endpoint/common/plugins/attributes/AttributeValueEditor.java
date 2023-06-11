/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes;

import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;


public interface AttributeValueEditor
{
	ComponentsContainer getEditor(AttributeEditContext editContext);

	String getCurrentValue() throws IllegalAttributeValueException;

	void setLabel(String label);
}
