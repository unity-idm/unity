/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.nimbusds.oauth2.sdk.Scope;

import pl.edu.icm.unity.base.utils.Log;

public class AttributeValueFilterUtils
{

	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, AttributeValueFilterUtils.class);

	private static final String FILTER_FROM_SCOPE_PREFIX = "claim_filter:";

	public static Scope getScopesWithoutFilterClaims(Scope scopes)
	{
		if (scopes == null)
			return null;
		
		return Scope.parse(scopes.stream()
				.map(scope -> scope.getValue())
				.filter(scope -> !scope.startsWith(FILTER_FROM_SCOPE_PREFIX))
				.toList());

	}

	public static List<AttributeFilteringSpec> getFiltersFromScopes(Scope scopes)
	{

		Map<String, Set<String>> filterByAttrName = new HashMap<>();
		if (scopes == null)
			return Collections.emptyList();
		
		List<String> filters = scopes.stream()
				.map(scope -> scope.getValue())
				.filter(scope -> scope.startsWith(FILTER_FROM_SCOPE_PREFIX))
				.toList();

		for (String filter : filters)
		{
			String attrAndValue = filter.replaceFirst(FILTER_FROM_SCOPE_PREFIX, "");
			if (!attrAndValue.contains(":"))
			{
				log.debug("Invalid claim value filter definition {}, skipping it", attrAndValue);
				continue;
			}

			String[] splittedFilter = attrAndValue.split(":", 2);
			if (!filterByAttrName.containsKey(splittedFilter[0]))
			{
				filterByAttrName.put(splittedFilter[0], new HashSet<>(List.of(splittedFilter[1])));
			} else
			{
				filterByAttrName.get(splittedFilter[0])
						.add(splittedFilter[1]);
			}
		}

		return filterByAttrName.entrySet()
				.stream()
				.map(e -> new AttributeFilteringSpec(e.getKey(), e.getValue()))
				.toList();
	}

	public static List<AttributeFilteringSpec> mergeFiltersWithPreservingLast(List<AttributeFilteringSpec> firstStageFilters,
			List<AttributeFilteringSpec> secondStageFilters)
	{
		if (secondStageFilters == null)
			return firstStageFilters;
		if (firstStageFilters == null)
			return secondStageFilters;

		Map<String, AttributeFilteringSpec> merged = firstStageFilters.stream()
				.collect(Collectors.toMap(f -> f.attributeName(), f -> f));
		for (AttributeFilteringSpec f : secondStageFilters)
		{
			if (merged.containsKey(f.attributeName()))
			{
				merged.remove(f.attributeName());
				merged.put(f.attributeName(), f);

			} else
			{
				merged.put(f.attributeName(), f);
			}
		}

		return merged.values()
				.stream()
				.toList();
	}
}
