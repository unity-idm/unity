/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.imunity.scim.config.DataArray;
import io.imunity.scim.config.DataArray.DataArrayType;
import io.imunity.scim.user.User;
import pl.edu.icm.unity.engine.api.AttributeValueConverter;
import pl.edu.icm.unity.engine.api.mvel.CachingMVELGroupProvider;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;

@RunWith(MockitoJUnitRunner.class)
public class DataArrayResolverTest
{
	@Mock
	private AttributeValueConverter attrValueConverter;

	private DataArrayResolver resolver;

	@Before
	public void init()
	{
		resolver = new DataArrayResolver(attrValueConverter);
	}

	@Test
	public void shouldConvertToAttributeArray() throws IllegalAttributeValueException
	{
		doReturn(List.of("value")).when(attrValueConverter).internalValuesToObjectValues(eq("attribute"),
				eq(List.of("value")));

		List<?> resolve = resolver.resolve(
				DataArray.builder().withType(DataArrayType.ATTRIBUTE).withValue("attribute").build(),
				EvaluatorContext.builder().withUser(User.builder()
						.withAttributes(List.of(new AttributeExt(
								new Attribute("attribute", StringAttributeSyntax.ID, "/", List.of("value")), false)))
						.build()).build());
		assertThat(resolve, is(List.of("value")));
	}

	@Test
	public void shouldConvertToIdentityArray() throws IllegalAttributeValueException
	{
		List<?> resolve = resolver
				.resolve(DataArray.builder().withType(DataArrayType.IDENTITY).withValue(UsernameIdentity.ID).build(),
						EvaluatorContext.builder().withUser(User.builder()
								.withIdentities(List.of(new Identity(UsernameIdentity.ID, "id1", 0, "id1"))).build())
								.build());
		assertThat(resolve, is(List.of("id1")));
	}

	@Test
	public void shouldConvertToMembershipArray() throws IllegalAttributeValueException
	{
		CachingMVELGroupProvider provider = new CachingMVELGroupProvider(
				Map.of("/g1", new Group("/g1"), "/g2", new Group("/g2")));

		List<?> resolve = resolver.resolve(DataArray.builder().withType(DataArrayType.MEMBERSHIP).build(),
				EvaluatorContext.builder().withGroupProvider(provider)

						.withUser(User.builder().withGroups(Set.of(new Group("/g1"), new Group("g2"))).build())
						.build());
		assertThat(resolve, is(List.of(provider.get("/g1"), provider.get("/g2"))));
	}
}
