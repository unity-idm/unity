/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.attributes;

import io.imunity.vaadin23.shared.endpoint.components.ComponentsContainer;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;


public interface AttributeValueEditor
{
	ComponentsContainer getEditor(AttributeEditContext editContext);

	String getCurrentValue() throws IllegalAttributeValueException;

	void setLabel(String label);
}
