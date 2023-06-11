/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directorySetup.attributeTypes;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;

/**
 * Implemented by all attribute type editors.
 * @author K. Benedyczak
 */
interface AttributeTypeEditor
{
	AttributeType getAttributeType() throws IllegalAttributeTypeException;
	Component getComponent();
}
