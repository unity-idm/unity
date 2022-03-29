/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.imunity.scim.schema.SCIMAttributeType;
import io.imunity.scim.user.User;
import pl.edu.icm.unity.engine.api.AttributeValueConverter;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Identity;

@RunWith(MockitoJUnitRunner.class)
public class DataConverterTest
{
	@Mock
	private AttributeValueConverter attrValueConverter;

	private UnityToSCIMDataConverter converter;

	@Before
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
		assertThat(converted.get(), is("value"));
	}

	@Test
	public void shouldConvertUserIdentityToStringType()
	{
		Optional<Object> converted = converter.convertUserIdentityToType(User.builder()
				.withIdentities(List.of(new Identity(UsernameIdentity.ID, "userName0", 0l, "userName0"))).build(),
				UsernameIdentity.ID, SCIMAttributeType.STRING);
		assertThat(converted.get(), is("userName0"));
	}

	@Test
	public void shouldConvertFromStringToBoolean() throws IllegalAttributeValueException
	{
		Object convertToType = converter.convertToType("true", SCIMAttributeType.BOOLEAN);
		assertThat(convertToType, is(true));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void shouldThrowExceptionWhenConvertFromUnsupportedTypeToBoolean()
	{
		converter.convertToType(Instant.now(), SCIMAttributeType.BOOLEAN);
	}

	@Test
	public void shouldConvertFromDateToDatetime()
	{
		Date date = new Date();
		Object convertToType = converter.convertToType(date, SCIMAttributeType.DATETIME);
		assertThat(convertToType, is(date.toInstant()));
	}

	@Test
	public void shouldConvertFromStringToDatetime()
	{
		String date = "2007-12-03T10:15:30.00Z";
		Object convertToType = converter.convertToType(date, SCIMAttributeType.DATETIME);
		assertThat(convertToType, is(Instant.parse(date)));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void shouldThrowExceptionWhenConvertFromUnsupportedTypeToDatetime()
	{
		converter.convertToType(Boolean.TRUE, SCIMAttributeType.DATETIME);
	}

}
