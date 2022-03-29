/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.junit.Test;

import io.imunity.scim.SCIMSystemScopeProvider;
import io.imunity.scim.config.AttributeDefinition;
import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.SCIMEndpointDescription;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.InvocationContext.InvocationMaterial;
import pl.edu.icm.unity.engine.api.authn.LoginSession;

public class AttributeFilterServiceTest
{

	@Test
	public void shouldNotFilterWhenDirectInvocationMaterial()
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 2, null, null, null, null));
		InvocationContext.setCurrent(context);

		SCIMEndpointDescription configuration = new SCIMEndpointDescription(URI.create("https://localhost:2443/scim"),
				"/scim", Collections.emptyList(), Collections.emptyList(), List.of("groupAttr1", "groupAttr2"));

		AttributeFilterService attributeFilterService = new AttributeFilterService(configuration);
		Predicate<AttributeDefinitionWithMapping> filter = attributeFilterService.getFilter();

		assertThat(
				filter.test(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("allow").build()).build()),
				is(true));
		assertThat(
				filter.test(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("groupAttr1").build()).build()),
				is(true));
	}

	@Test
	public void shouldAllowOnlyGroupMembershipAttrsWhenOAuthInvocationMaterialAndReadMembershipScope()
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 2, null, null, null, null));
		context.setInvocationMaterial(InvocationMaterial.OAUTH_DELEGATION);
		context.setScopes(List.of(SCIMSystemScopeProvider.READ_MEMBERSHIPS_SCOPE));
		InvocationContext.setCurrent(context);

		SCIMEndpointDescription configuration = new SCIMEndpointDescription(URI.create("https://localhost:2443/scim"),
				"/scim", Collections.emptyList(), Collections.emptyList(), List.of("groupAttr1", "groupAttr2"));

		AttributeFilterService attributeFilterService = new AttributeFilterService(configuration);
		Predicate<AttributeDefinitionWithMapping> filter = attributeFilterService.getFilter();

		assertThat(
				filter.test(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("deny").build()).build()),
				is(false));
		assertThat(
				filter.test(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("groupAttr1").build()).build()),
				is(true));
		assertThat(
				filter.test(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("groupAttr2").build()).build()),
				is(true));
	}

	@Test
	public void shouldAllowOnlyNotGroupMembershipAttrsWhenOAuthInvocationMaterialAndReadProfileScope()
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 2, null, null, null, null));
		context.setInvocationMaterial(InvocationMaterial.OAUTH_DELEGATION);
		context.setScopes(List.of(SCIMSystemScopeProvider.READ_PROFILE_SCOPE));
		InvocationContext.setCurrent(context);

		SCIMEndpointDescription configuration = new SCIMEndpointDescription(URI.create("https://localhost:2443/scim"),
				"/scim", Collections.emptyList(), Collections.emptyList(), List.of("groupAttr1", "groupAttr2"));

		AttributeFilterService attributeFilterService = new AttributeFilterService(configuration);
		Predicate<AttributeDefinitionWithMapping> filter = attributeFilterService.getFilter();

		assertThat(
				filter.test(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("allow").build()).build()),
				is(true));
		assertThat(
				filter.test(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("groupAttr1").build()).build()),
				is(false));
		assertThat(
				filter.test(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("groupAttr2").build()).build()),
				is(false));
	}

	@Test
	public void shouldNotFilterWhenOAuthInvocationMaterialAndAllScimScopes()
	{
		InvocationContext context = new InvocationContext(null, null, null);
		context.setLoginSession(new LoginSession(null, null, 0, 2, null, null, null, null));
		context.setInvocationMaterial(InvocationMaterial.OAUTH_DELEGATION);
		context.setScopes(List.of(SCIMSystemScopeProvider.READ_PROFILE_SCOPE, SCIMSystemScopeProvider.READ_MEMBERSHIPS_SCOPE));
		InvocationContext.setCurrent(context);

		SCIMEndpointDescription configuration = new SCIMEndpointDescription(URI.create("https://localhost:2443/scim"),
				"/scim", Collections.emptyList(), Collections.emptyList(), List.of("groupAttr1", "groupAttr2"));

		AttributeFilterService attributeFilterService = new AttributeFilterService(configuration);
		Predicate<AttributeDefinitionWithMapping> filter = attributeFilterService.getFilter();

		assertThat(
				filter.test(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("allow").build()).build()),
				is(true));
		assertThat(
				filter.test(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("groupAttr1").build()).build()),
				is(true));
		assertThat(
				filter.test(AttributeDefinitionWithMapping.builder()
						.withAttributeDefinition(AttributeDefinition.builder().withName("groupAttr2").build()).build()),
				is(true));
	}
}
