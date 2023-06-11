/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.idp;

import java.util.*;
import java.util.stream.Collectors;

import pl.edu.icm.unity.engine.api.attributes.DynamicAttribute;

public class ActiveValueClientHelper
{
	public static Optional<ActiveValueSelectionConfig> getActiveValueSelectionConfig(Set<ActiveValueClient> activeValueClients,
	                                                                                                     String client, Collection<DynamicAttribute> allAttributes)
	{
		Optional<String> key = getActiveValueSelectionConfigKey(activeValueClients, client);
		return key.isPresent() ? getActiveValueSelectionConfigFromKey(activeValueClients, key.get(), allAttributes) : Optional.empty();
	}

	public static boolean isActiveValueSelectionConfiguredForClient(Set<ActiveValueClient> activeValueClients, String client)
	{
		return getActiveValueSelectionConfigKey(activeValueClients, client).isPresent();
	}

	private static Optional<String> getActiveValueSelectionConfigKey(Set<ActiveValueClient> activeValueClients, String client)
	{
		String defaultClientKey = null;
		for (ActiveValueClient activeValueClient: activeValueClients)
		{
			if (activeValueClient.client == null)
			{
				defaultClientKey = activeValueClient.key;
				continue;
			}
			if (activeValueClient.client.equals(client))
			{
				return Optional.of(activeValueClient.key);
			}
		}
		return Optional.ofNullable(defaultClientKey);
	}

	private static Optional<ActiveValueSelectionConfig> getActiveValueSelectionConfigFromKey(Set<ActiveValueClient> activeValueClients,
	                                                                                                             String key, Collection<DynamicAttribute> attributes)
	{
		Map<String, DynamicAttribute> attrsMap = attributes.stream()
				.collect(Collectors.toMap(da -> da.getAttribute().getName(), da -> da));

		List<String> singleValueAttributes = activeValueClients.stream()
				.filter(client -> client.key.equals(key))
				.flatMap(client -> client.singleValueAttributes.stream())
				.collect(Collectors.toList());

		List<String> multiValueAttributes = activeValueClients.stream()
				.filter(client -> client.key.equals(key))
				.flatMap(client -> client.multiValueAttributes.stream())
				.collect(Collectors.toList());

		List<DynamicAttribute> singleSelectable = getAttributeForSelection(singleValueAttributes, attrsMap);
		List<DynamicAttribute> multiSelectable = getAttributeForSelection(multiValueAttributes, attrsMap);
		if (singleSelectable.isEmpty() && multiSelectable.isEmpty())
			return Optional.empty();
		List<DynamicAttribute> remaining = new ArrayList<>(attributes);
		remaining.removeAll(singleSelectable);
		remaining.removeAll(multiSelectable);
		return Optional.of(new ActiveValueSelectionConfig(multiSelectable, singleSelectable, remaining));
	}

	private static List<DynamicAttribute> getAttributeForSelection(List<String> names,
	                                                               Map<String, DynamicAttribute> attributes)
	{
		return names.stream()
				.map(attr -> attributes.get(attr))
				.filter(attr -> attr != null)
				.collect(Collectors.toList());
	}

	public static class ActiveValueSelectionConfig
	{
		public final List<DynamicAttribute> multiSelectableAttributes;
		public final List<DynamicAttribute> singleSelectableAttributes;
		public final List<DynamicAttribute> remainingAttributes;

		public ActiveValueSelectionConfig(List<DynamicAttribute> multiSelectableAttributes,
		                                  List<DynamicAttribute> singleSelectableAttributes,
		                                  List<DynamicAttribute> remainingAttributes)
		{
			this.multiSelectableAttributes = multiSelectableAttributes;
			this.singleSelectableAttributes = singleSelectableAttributes;
			this.remainingAttributes = remainingAttributes;
		}
	}
}
