/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directorySetup.attributeTypes;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Implemented by all attribute type editors.
 * @author K. Benedyczak
 */
interface AttributeTypeEditor
{
	AttributeType getAttributeType() throws IllegalAttributeTypeException;
	Component getComponent();
}
