/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.tx.Transactional;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeCacheValidationReport;
import pl.edu.icm.unity.engine.api.attributes.AttributeCacheValidationService;
import pl.edu.icm.unity.engine.api.attributes.AttributeMismatch;
import pl.edu.icm.unity.engine.api.attributes.EntityAttributeMismatches;
import pl.edu.icm.unity.engine.api.attributes.GroupValidationReport;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.group.GroupNotFoundException;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.store.api.AttributesCacheDAO;
import pl.edu.icm.unity.store.api.AttributesCachePendingDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.types.EntityInGroup;

@Component
class AttributeCacheValidationServiceImpl implements AttributeCacheValidationService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, AttributeCacheValidationServiceImpl.class);

	private final InternalAuthorizationManager authz;
	private final GroupDAO groupDAO;
	private final BulkGroupQueryService bulkGroupQueryService;
	private final AttributesCacheDAO attributesCacheDAO;
	private final AttributesCachePendingDAO attributesCachePendingDAO;

	@Autowired
	AttributeCacheValidationServiceImpl(InternalAuthorizationManager authz, GroupDAO groupDAO,
			BulkGroupQueryService bulkGroupQueryService, AttributesCacheDAO attributesCacheDAO,
			AttributesCachePendingDAO attributesCachePendingDAO)
	{
		this.authz = authz;
		this.groupDAO = groupDAO;
		this.bulkGroupQueryService = bulkGroupQueryService;
		this.attributesCacheDAO = attributesCacheDAO;
		this.attributesCachePendingDAO = attributesCachePendingDAO;
	}

	@Override
	@Transactional
	public AttributeCacheValidationReport validate(Optional<String> group) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);

		List<String> groups;
		if (group.isPresent())
		{
			if (!groupDAO.exists(group.get()))
				throw new GroupNotFoundException("Group " + group.get() + " does not exist");
			groups = List.of(group.get());
		} else
		{
			groups = new ArrayList<>(groupDAO.getAllNames());
		}
		log.trace("Self-validation requested for {} group(s): {}", groups.size(), groups);

		List<GroupValidationReport> reports = new ArrayList<>();
		for (String g : groups)
			reports.add(validateGroup(g));
		log.trace("Self-validation finished for {} group(s)", groups.size());
		return new AttributeCacheValidationReport(reports);
	}

	private GroupValidationReport validateGroup(String group) throws EngineException
	{
		GroupMembershipData data = bulkGroupQueryService.getBulkMembershipData(group);
		Map<Long, Map<String, AttributeExt>> fresh = bulkGroupQueryService.getGroupUsersAttributes(group, data);

		Set<Long> pending = attributesCachePendingDAO.getPendingForGroup(group).stream()
				.map(EntityInGroup::entityId)
				.collect(Collectors.toSet());
		Map<Long, Map<String, AttributeExt>> cached =
				StoredAttributesGrouping.byEntity(attributesCacheDAO.getGroupAttributes(group));
		log.trace("Validating group {}: {} fresh entities, {} pending, {} cached entities", group, fresh.size(),
				pending.size(), cached.size());

		List<Long> entitiesWithPendingUpdate = new ArrayList<>(pending);
		List<EntityAttributeMismatches> entitiesWithMismatches = new ArrayList<>();

		for (Map.Entry<Long, Map<String, AttributeExt>> entry : fresh.entrySet())
		{
			long entityId = entry.getKey();
			if (pending.contains(entityId))
				continue;
			List<AttributeMismatch> mismatches = compare(cached.getOrDefault(entityId, Map.of()), entry.getValue());
			if (!mismatches.isEmpty())
			{
				log.trace("Group {}: mismatch found for entity {}: {}", group, entityId, mismatches);
				entitiesWithMismatches.add(new EntityAttributeMismatches(entityId, mismatches));
			}
		}

		log.trace("Group {} validated: {} pending, {} entities with mismatches", group,
				entitiesWithPendingUpdate.size(), entitiesWithMismatches.size());
		return new GroupValidationReport(group, entitiesWithPendingUpdate, entitiesWithMismatches);
	}

	private List<AttributeMismatch> compare(Map<String, AttributeExt> cached, Map<String, AttributeExt> fresh)
	{
		Set<String> attributeNames = new HashSet<>(cached.keySet());
		attributeNames.addAll(fresh.keySet());

		List<AttributeMismatch> mismatches = new ArrayList<>();
		for (String name : attributeNames)
		{
			List<String> cachedValues = cached.containsKey(name) ? cached.get(name).getValues() : List.of();
			List<String> freshValues = fresh.containsKey(name) ? fresh.get(name).getValues() : List.of();
			if (!cachedValues.equals(freshValues))
				mismatches.add(new AttributeMismatch(name, cachedValues, freshValues));
		}
		return mismatches;
	}
}
