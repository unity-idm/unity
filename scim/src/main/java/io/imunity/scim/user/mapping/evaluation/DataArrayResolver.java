/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.scim.config.DataArray;
import pl.edu.icm.unity.engine.api.AttributeValueConverter;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.types.basic.AttributeExt;

@Component
class DataArrayResolver
{
	private final AttributeValueConverter attrValueConverter;

	@Autowired
	DataArrayResolver(AttributeValueConverter attrValueConverter)
	{
		this.attrValueConverter = attrValueConverter;
	}

	List<?> resolve(DataArray dataArray, EvaluatorContext context) throws IllegalAttributeValueException
	{
		if (dataArray == null || dataArray.type == null)
		{
			return Collections.emptyList();
		}

		switch (dataArray.type)
		{
		case ATTRIBUTE:
			Optional<AttributeExt> attribute = context.user.attributes.stream()
					.filter(a -> a.getName().equals(dataArray.value.get())).findAny();
			if (attribute.isEmpty())
				return Collections.emptyList();
			return attrValueConverter.internalValuesToObjectValues(attribute.get().getName(),
					attribute.get().getValues());

		case IDENTITY:
			return context.user.identities.stream().filter(a -> a.getTypeId().equals(dataArray.value.get()))
					.map(i -> i.getValue()).collect(Collectors.toList());

		case MEMBERSHIP:
			return context.user.groups.stream().map(g -> context.groupProvider.get(g.getPathEncoded()))
					.collect(Collectors.toList());

		default:

			return Collections.emptyList();
		}
	}
}
