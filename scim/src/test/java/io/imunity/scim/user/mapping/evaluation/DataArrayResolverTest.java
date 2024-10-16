/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.imunity.scim.config.DataArray;
import io.imunity.scim.config.DataArray.DataArrayType;
import io.imunity.scim.user.User;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.engine.api.AttributeValueConverter;
import pl.edu.icm.unity.engine.api.mvel.CachingMVELGroupProvider;
import pl.edu.icm.unity.engine.api.mvel.MVELGroup;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

@ExtendWith(MockitoExtension.class)
public class DataArrayResolverTest
{
	@Mock
	private AttributeValueConverter attrValueConverter;

	private DataArrayResolver resolver;

	@BeforeEach
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
		assertThat(resolve).isEqualTo(List.of("value"));
	}

	@Test
	public void shouldConvertToIdentityArray() throws IllegalAttributeValueException
	{
		List<?> resolve = resolver
				.resolve(DataArray.builder().withType(DataArrayType.IDENTITY).withValue(UsernameIdentity.ID).build(),
						EvaluatorContext.builder().withUser(User.builder()
								.withIdentities(List.of(new Identity(UsernameIdentity.ID, "id1", 0, "id1"))).build())
								.build());
		assertThat(resolve).isEqualTo(List.of("id1"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldConvertToMembershipArray() throws IllegalAttributeValueException
	{
		CachingMVELGroupProvider provider = new CachingMVELGroupProvider(
				Map.of("/g1", new Group("/g1"), "/g2", new Group("/g2")));

		List<?> resolve = resolver.resolve(DataArray.builder().withType(DataArrayType.MEMBERSHIP).build(),
				EvaluatorContext.builder().withGroupProvider(provider)

						.withUser(User.builder().withGroups(Set.of(new Group("/g1"), new Group("g2"))).build())
						.build());
		assertThat((List<MVELGroup>) resolve).contains(provider.get("/g1"), provider.get("/g2"));
	}
}
