/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.imunity.rest.api.RestAttributeCacheValidationReport;
import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.engine.api.attributes.AttributeCacheValidationReport;
import pl.edu.icm.unity.engine.api.attributes.AttributeCacheValidationService;
import pl.edu.icm.unity.engine.api.attributes.AttributeMismatch;
import pl.edu.icm.unity.engine.api.attributes.EntityAttributeMismatches;
import pl.edu.icm.unity.engine.api.attributes.GroupValidationReport;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.group.GroupNotFoundException;

@ExtendWith(MockitoExtension.class)
class AttributeCacheValidationRESTAdminTest
{
	private static final AttributeCacheValidationReport EMPTY = new AttributeCacheValidationReport(List.of());

	@Mock
	private AttributeCacheValidationService validationService;

	private AttributeCacheValidationRESTAdmin handler;

	@BeforeEach
	void setUp()
	{
		handler = new AttributeCacheValidationRESTAdmin(validationService);
	}

	@Test
	void shouldValidateAllGroupsWhenGroupOmitted() throws Exception
	{
		when(validationService.validate(Optional.empty())).thenReturn(EMPTY);

		handler.validate(null);
	}

	@Test
	void shouldPassAbsoluteGroupPathUnchanged() throws Exception
	{
		when(validationService.validate(Optional.of("/A/B"))).thenReturn(EMPTY);

		handler.validate("/A/B");
	}

	@Test
	void shouldNormalizeRelativeGroupPath() throws Exception
	{
		when(validationService.validate(Optional.of("/A/B"))).thenReturn(EMPTY);

		handler.validate("A/B");
	}

	@Test
	void shouldSerializeCompleteReport() throws Exception
	{
		AttributeCacheValidationReport report = new AttributeCacheValidationReport(List.of(
				new GroupValidationReport("/A", List.of(5L), List.of(new EntityAttributeMismatches(1L, List.of(
						new AttributeMismatch("attr", List.of("stale"), List.of("fresh"), false),
						AttributeMismatch.redacted("sys:Credential:password"))))),
				new GroupValidationReport("/B", List.of(), List.of())));
		when(validationService.validate(Optional.empty())).thenReturn(report);

		String json = handler.validate(null);

		RestAttributeCacheValidationReport parsed = Constants.MAPPER.readValue(json,
				RestAttributeCacheValidationReport.class);
		assertThat(parsed.groups).hasSize(2);
		var groupA = parsed.groups.get(0);
		assertThat(groupA.group).isEqualTo("/A");
		assertThat(groupA.entitiesWithPendingUpdate).containsExactly(5L);
		var mismatches = groupA.entitiesWithMismatches.get(0);
		assertThat(mismatches.entityId).isEqualTo(1L);
		assertThat(mismatches.mismatches.get(0).attributeName).isEqualTo("attr");
		assertThat(mismatches.mismatches.get(0).cachedValues).containsExactly("stale");
		assertThat(mismatches.mismatches.get(0).freshValues).containsExactly("fresh");
		assertThat(mismatches.mismatches.get(0).valuesRedacted).isFalse();
		assertThat(mismatches.mismatches.get(1).attributeName).isEqualTo("sys:Credential:password");
		assertThat(mismatches.mismatches.get(1).valuesRedacted).isTrue();
		assertThat(mismatches.mismatches.get(1).cachedValues).isEmpty();
		var groupB = parsed.groups.get(1);
		assertThat(groupB.group).isEqualTo("/B");
		assertThat(groupB.entitiesWithPendingUpdate).isEmpty();
		assertThat(groupB.entitiesWithMismatches).isEmpty();
	}

	@Test
	void shouldPropagateNotFoundGroup() throws Exception
	{
		when(validationService.validate(Optional.of("/missing")))
				.thenThrow(new GroupNotFoundException("no such group"));

		assertThatThrownBy(() -> handler.validate("/missing")).isInstanceOf(GroupNotFoundException.class);
	}

	@Test
	void shouldPropagateAuthorizationFailure() throws Exception
	{
		when(validationService.validate(Optional.empty())).thenThrow(new AuthorizationException("denied"));

		assertThatThrownBy(() -> handler.validate(null)).isInstanceOf(AuthorizationException.class);
	}
}
