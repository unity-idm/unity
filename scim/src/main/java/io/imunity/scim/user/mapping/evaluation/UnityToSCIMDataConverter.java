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

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import io.imunity.scim.schema.SCIMAttributeType;
import io.imunity.scim.user.User;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.entity.Identity;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeValueConverter;

@Component
class UnityToSCIMDataConverter
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, UnityToSCIMDataConverter.class);
	
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
			return Optional.ofNullable(convertToType(attrValueConverter
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
			return Optional.ofNullable(convertToType(identity.get().getValue(), type));
		}

		return Optional.empty();
	}

	Object convertToType(Object value, SCIMAttributeType type)
	{
		if (value == null)
			return null;

		switch (type)
		{
		case STRING:
			return convertToString(value);
		case BOOLEAN:
			return convertToBoolean(value);
		case DATETIME:
			return convertToDateTime(value);
		default:
			log.warn("Can not convert from " + value.getClass() + " to " + type);
			return null;
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

		log.warn("Can not convert to date from " + value.getClass());
		return null;
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
			String booleanValue = (String) value;
			if (booleanValue.equalsIgnoreCase("true") || booleanValue.equalsIgnoreCase("false"))
			{
				return Boolean.valueOf(booleanValue);
			}
		}
		log.warn("Can not convert to boolean from " + value.getClass() + ", value=" + value);
		return null;
	}
}
