/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.identities;

import io.imunity.vaadin23.shared.endpoint.components.ComponentsContainer;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.types.basic.IdentityParam;


public interface IdentityEditor
{
	ComponentsContainer getEditor(IdentityEditorContext context);
	IdentityParam getValue() throws IllegalIdentityValueException;
	void setDefaultValue(IdentityParam value);
	void setLabel(String value);
}
