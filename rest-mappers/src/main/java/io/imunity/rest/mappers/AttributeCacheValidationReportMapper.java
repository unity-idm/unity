/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.rest.mappers;

import java.util.stream.Collectors;

import io.imunity.rest.api.RestAttributeCacheValidationReport;
import io.imunity.rest.api.RestAttributeMismatch;
import io.imunity.rest.api.RestEntityAttributeMismatches;
import io.imunity.rest.api.RestGroupValidationReport;
import pl.edu.icm.unity.engine.api.attributes.AttributeCacheValidationReport;
import pl.edu.icm.unity.engine.api.attributes.AttributeMismatch;
import pl.edu.icm.unity.engine.api.attributes.EntityAttributeMismatches;
import pl.edu.icm.unity.engine.api.attributes.GroupValidationReport;

public class AttributeCacheValidationReportMapper
{
	public static RestAttributeCacheValidationReport map(AttributeCacheValidationReport report)
	{
		return RestAttributeCacheValidationReport.builder()
				.withGroups(report.groups().stream()
						.map(AttributeCacheValidationReportMapper::map)
						.collect(Collectors.toList()))
				.build();
	}

	private static RestGroupValidationReport map(GroupValidationReport report)
	{
		return RestGroupValidationReport.builder()
				.withGroup(report.group())
				.withEntitiesWithPendingUpdate(report.entitiesWithPendingUpdate())
				.withEntitiesWithMismatches(report.entitiesWithMismatches().stream()
						.map(AttributeCacheValidationReportMapper::map)
						.collect(Collectors.toList()))
				.build();
	}

	private static RestEntityAttributeMismatches map(EntityAttributeMismatches mismatches)
	{
		return RestEntityAttributeMismatches.builder()
				.withEntityId(mismatches.entityId())
				.withMismatches(mismatches.mismatches().stream()
						.map(AttributeCacheValidationReportMapper::map)
						.collect(Collectors.toList()))
				.build();
	}

	private static RestAttributeMismatch map(AttributeMismatch mismatch)
	{
		return RestAttributeMismatch.builder()
				.withAttributeName(mismatch.attributeName())
				.withCachedValues(mismatch.cachedValues())
				.withFreshValues(mismatch.freshValues())
				.build();
	}
}
