/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.rest.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.imunity.rest.api.RestAttributeCacheValidationReport;
import io.imunity.rest.api.RestAttributeMismatch;
import io.imunity.rest.api.RestEntityAttributeMismatches;
import io.imunity.rest.api.RestGroupValidationReport;
import pl.edu.icm.unity.engine.api.attributes.AttributeCacheValidationReport;
import pl.edu.icm.unity.engine.api.attributes.AttributeMismatch;
import pl.edu.icm.unity.engine.api.attributes.EntityAttributeMismatches;
import pl.edu.icm.unity.engine.api.attributes.GroupValidationReport;

public class AttributeCacheValidationReportMapperTest
		extends OneWayMapperTestBase<AttributeCacheValidationReport, RestAttributeCacheValidationReport>
{
	@Override
	protected AttributeCacheValidationReport getAPIObject()
	{
		return new AttributeCacheValidationReport(List.of(new GroupValidationReport("/A", List.of(2L),
				List.of(new EntityAttributeMismatches(1L, List.of(
						new AttributeMismatch("attr", List.of("stale"), List.of("fresh"), false),
						AttributeMismatch.redacted("sys:Credential:password")))))));
	}

	@Override
	protected RestAttributeCacheValidationReport getRestObject()
	{
		return RestAttributeCacheValidationReport.builder()
				.withGroups(List.of(RestGroupValidationReport.builder()
						.withGroup("/A")
						.withEntitiesWithPendingUpdate(List.of(2L))
						.withEntitiesWithMismatches(List.of(RestEntityAttributeMismatches.builder()
								.withEntityId(1L)
								.withMismatches(List.of(
										RestAttributeMismatch.builder()
												.withAttributeName("attr")
												.withCachedValues(List.of("stale"))
												.withFreshValues(List.of("fresh"))
												.build(),
										RestAttributeMismatch.builder()
												.withAttributeName("sys:Credential:password")
												.withValuesRedacted(true)
												.build()))
								.build()))
						.build()))
				.build();
	}

	@Override
	protected Function<AttributeCacheValidationReport, RestAttributeCacheValidationReport> getMapper()
	{
		return AttributeCacheValidationReportMapper::map;
	}

	@Test
	public void shouldMapEmptyReportsAndLists()
	{
		AttributeCacheValidationReport report = new AttributeCacheValidationReport(List.of(
				new GroupValidationReport("/A", List.of(), List.of())));

		RestAttributeCacheValidationReport mapped = AttributeCacheValidationReportMapper.map(report);

		assertThat(mapped).isEqualTo(RestAttributeCacheValidationReport.builder()
				.withGroups(List.of(RestGroupValidationReport.builder().withGroup("/A").build()))
				.build());
		assertThat(AttributeCacheValidationReportMapper.map(new AttributeCacheValidationReport(List.of())).groups)
				.isEmpty();
	}
}
