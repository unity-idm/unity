/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.consent_utils;

import pl.edu.icm.unity.base.attribute.Attribute;

import java.util.Collection;
import java.util.Map;

public interface SelectableAttributesComponent
{

	/**
	 * @return collection of attributes without the ones hidden by the user.
	 */
	Collection<Attribute> getUserFilteredAttributes();

	/**
	 * @return collection of attributes with values hidden by the user.
	 */
	Map<String, Attribute> getHiddenAttributes();

	void setInitialState(Map<String, Attribute> savedState);

}