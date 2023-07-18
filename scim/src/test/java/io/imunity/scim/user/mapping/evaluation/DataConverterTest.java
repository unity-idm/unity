/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.imunity.scim.schema.SCIMAttributeType;
import io.imunity.scim.user.User;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.engine.api.AttributeValueConverter;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

@ExtendWith(MockitoExtension.class)
public class DataConverterTest
{
	@Mock
	private AttributeValueConverter attrValueConverter;

	private UnityToSCIMDataConverter converter;

	@BeforeEach
	public void init()
	{
		converter = new UnityToSCIMDataConverter(attrValueConverter);
	}

	@Test
	public void shouldConvertUserAttributeToStringType() throws IllegalAttributeValueException
	{
		doReturn(List.of("value")).when(attrValueConverter).internalValuesToObjectValues(eq("attrName"),
				eq(List.of("value")));
		Optional<Object> converted = converter.convertUserAttributeToType(User.builder()
				.withAttributes(List.of(new AttributeExt(
						new Attribute("attrName", StringAttributeSyntax.ID, "/", List.of("value")), false)))
				.build(), "attrName", SCIMAttributeType.STRING);
		assertThat(converted.get()).isEqualTo("value");
	}

	@Test
	public void shouldConvertUserIdentityToStringType()
	{
		Optional<Object> converted = converter.convertUserIdentityToType(User.builder()
				.withIdentities(List.of(new Identity(UsernameIdentity.ID, "userName0", 0l, "userName0"))).build(),
				UsernameIdentity.ID, SCIMAttributeType.STRING);
		assertThat(converted.get()).isEqualTo("userName0");
	}

	@Test
	public void shouldConvertFromStringToBoolean() throws IllegalAttributeValueException
	{
		Object convertToType = converter.convertToType("true", SCIMAttributeType.BOOLEAN);
		assertThat(convertToType).isEqualTo(true);
	}

	@Test
	public void shouldConvertFromDateToDatetime()
	{
		Date date = new Date();
		Object convertToType = converter.convertToType(date, SCIMAttributeType.DATETIME);
		assertThat(convertToType).isEqualTo(date.toInstant());
	}

	@Test
	public void shouldConvertFromStringToDatetime()
	{
		String date = "2007-12-03T10:15:30.00Z";
		Object convertToType = converter.convertToType(date, SCIMAttributeType.DATETIME);
		assertThat(convertToType).isEqualTo(Instant.parse(date));
	}

}
