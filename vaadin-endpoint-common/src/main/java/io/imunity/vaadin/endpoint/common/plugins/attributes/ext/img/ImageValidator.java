/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.plugins.attributes.ext.img;

import pl.edu.icm.unity.attr.UnityImage;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;

interface ImageValidator
{
	void validate(UnityImage value) throws IllegalAttributeValueException;
}
