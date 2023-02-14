/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.identities;

import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.types.basic.IdentityParam;


public interface IdentityEditor
{
	ComponentsContainer getEditor(IdentityEditorContext context);
	IdentityParam getValue() throws IllegalIdentityValueException;
	void setDefaultValue(IdentityParam value);
	void setLabel(String value);
}
