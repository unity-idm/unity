/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.plugins.attributes.ext.img;

import pl.edu.icm.unity.base.attr.UnityImage;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;

interface ImageValidator
{
	void validate(UnityImage value) throws IllegalAttributeValueException;
}
