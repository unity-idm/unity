/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.nimbusds.oauth2.sdk.Scope;

public class AttributeValueFilterUtilsTest
{
	@Test
	public void shouldParseSigleValueFilter()
	{
		Scope scope = Scope.parse(List.of("scope1", "scope2",
				"claim_filter:entitlements:urn:geant:dfn.de:nfdi.de:punch:group:PUNCH4NFDI:elsa_one:elsa_one_adm#http://login.helmholtz.de:punch-aai"));

		List<AttributeFilteringSpec> filtersFromScopes = AttributeValueFilterUtils.getFiltersFromScopes(scope);
		assertThat(filtersFromScopes.get(0)).isEqualTo(new AttributeFilteringSpec("entitlements", Set.of(
				"urn:geant:dfn.de:nfdi.de:punch:group:PUNCH4NFDI:elsa_one:elsa_one_adm#http://login.helmholtz.de:punch-aai")));
	}

	@Test
	public void shouldParseMultiValueFilter()
	{
		Scope scope = Scope
				.parse(List.of("scope1", "scope2", "claim_filter:entitlements:val1", "claim_filter:entitlements:val2"));

		List<AttributeFilteringSpec> filtersFromScopes = AttributeValueFilterUtils.getFiltersFromScopes(scope);
		assertThat(filtersFromScopes.get(0))
				.isEqualTo(new AttributeFilteringSpec("entitlements", Set.of("val1", "val2")));
	}

	@Test
	public void shouldReturnNotFilterScope()
	{
		Scope scope = Scope.parse(List.of("scope1", "scope2", "claim_filter:entitlements:demo"));
		Scope notFilterScopes = AttributeValueFilterUtils.getScopesWithoutFilterClaims(scope);
		assertThat(notFilterScopes).isEqualTo(Scope.parse(List.of("scope1", "scope2")));
	}

	@Test
	public void shouldMergeFiltersAndOverwriteByLast()
	{

		List<AttributeFilteringSpec> merged = AttributeValueFilterUtils.mergeFiltersWithPreservingLast(
				List.of(new AttributeFilteringSpec("a", Set.of("a1", "a2")),
						new AttributeFilteringSpec("b", Set.of("b1", "b2")), new AttributeFilteringSpec("c", Set.of("c1"))),
				List.of(new AttributeFilteringSpec("a", Set.of("a1")),
						new AttributeFilteringSpec("b", Set.of("b1", "b2")),
						new AttributeFilteringSpec("d", Set.of("d1"))));
		assertThat(merged).isEqualTo(List.of(new AttributeFilteringSpec("a", Set.of("a1")),
				new AttributeFilteringSpec("b", Set.of("b1", "b2")),
				new AttributeFilteringSpec("c", Set.of("c1")),
				new AttributeFilteringSpec("d", Set.of("d1"))));
	}
}
