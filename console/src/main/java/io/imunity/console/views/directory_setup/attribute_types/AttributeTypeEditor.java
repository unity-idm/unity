/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_setup.attribute_types;

import com.vaadin.flow.component.Component;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;

/**
 * Implemented by all attribute type editors.
 * @author P.Piernik
 */
interface AttributeTypeEditor
{
	AttributeType getAttributeType() throws IllegalAttributeTypeException;
	Component getComponent();
}
