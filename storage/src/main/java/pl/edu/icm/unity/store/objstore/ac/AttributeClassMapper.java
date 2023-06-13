/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.ac;

import pl.edu.icm.unity.base.attribute.AttributesClass;

class AttributeClassMapper
{
	static DBAttributesClass map(AttributesClass attributesClass)
	{
		return DBAttributesClass.builder()
				.withName(attributesClass.getName())
				.withDescription(attributesClass.getDescription())
				.withAllowed(attributesClass.getAllowed())
				.withMandatory(attributesClass.getMandatory())
				.withAllowArbitrary(attributesClass.isAllowArbitrary())
				.withParentClasses(attributesClass.getParentClasses())
				.build();
	}

	static AttributesClass map(DBAttributesClass dbAttributesClass)
	{
		return new AttributesClass(dbAttributesClass.name, dbAttributesClass.description, dbAttributesClass.allowed,
				dbAttributesClass.mandatory, dbAttributesClass.allowArbitrary, dbAttributesClass.parentClasses);
	}
}
