/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;

import org.springframework.stereotype.Component;

import io.imunity.scim.schema.SCIMAttributeType;
import io.imunity.scim.user.User;
import pl.edu.icm.unity.engine.api.AttributeValueConverter;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Identity;

@Component
class UnityToSCIMDataConverter
{
	private final AttributeValueConverter attrValueConverter;

	UnityToSCIMDataConverter(AttributeValueConverter attrValueConverter)
	{
		this.attrValueConverter = attrValueConverter;
	}

	Optional<Object> convertUserAttributeToType(User user, String attributeName, SCIMAttributeType type)
			throws IllegalAttributeValueException
	{
		Optional<AttributeExt> attribute = user.attributes.stream().filter(a -> a.getName().equals(attributeName))
				.findFirst();
		if (attribute.isPresent() && !attribute.get().getValues().isEmpty())
		{
			return Optional.of(convertToType(attrValueConverter
					.internalValuesToObjectValues(attribute.get().getName(), attribute.get().getValues()).get(0),
					type));
		}
		return Optional.empty();
	}

	Optional<Object> convertUserIdentityToType(User user, String identityType, SCIMAttributeType type)
	{
		Optional<Identity> identity = user.identities.stream().filter(a -> a.getTypeId().equals(identityType))
				.findFirst();
		if (identity.isPresent() && !identity.get().getValue().isEmpty())
		{
			return Optional.of(convertToType(identity.get().getValue(), type));
		}

		return Optional.empty();
	}

	Object convertToType(Object value, SCIMAttributeType type)
	{
		switch (type)
		{
		case STRING:
			return convertToString(value);
		case BOOLEAN:
			return convertToBoolean(value);
		case DATETIME:
			return convertToDateTime(value);
		default:
			throw new UnsupportedOperationException("Can not convert from " + value.getClass() + " to " + type);
		}
	}

	private Instant convertToDateTime(Object value)
	{
		if (value instanceof Date)
		{
			return ((Date) value).toInstant();
		} else if (value instanceof LocalDate)
		{
			return Instant.from((LocalDate) value);
		} else if (value instanceof LocalDateTime)
		{
			return ((LocalDateTime) value).toInstant(ZoneOffset.UTC);
		} else if (value instanceof ZonedDateTime)
		{
			return ((ZonedDateTime) value).toInstant();
		} else if (value instanceof String)
		{
			return Instant.parse((String) value);
		}

		throw new UnsupportedOperationException("Can not convert to date from " + value.getClass());
	}

	private String convertToString(Object value)
	{
		return value.toString();
	}

	private Boolean convertToBoolean(Object value)
	{
		if (value instanceof Boolean)
		{
			return (Boolean) value;
		} else if (value instanceof String)
		{
			return Boolean.valueOf((String) value);
		}
		throw new UnsupportedOperationException("Can not convert to boolean from " + value.getClass());
	}
}
