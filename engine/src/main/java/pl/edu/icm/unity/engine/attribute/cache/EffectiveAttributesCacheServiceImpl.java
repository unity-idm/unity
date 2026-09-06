/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.base.tx.Transactional;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.CachedAttributes;
import pl.edu.icm.unity.engine.api.attributes.EffectiveAttributesCacheService;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.store.api.AttributesCacheDAO;
import pl.edu.icm.unity.store.api.AttributesCachePendingDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.types.EntityInGroup;
import pl.edu.icm.unity.store.types.StoredAttribute;

@Component
class EffectiveAttributesCacheServiceImpl implements EffectiveAttributesCacheService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, EffectiveAttributesCacheServiceImpl.class);

	private final AttributesCacheDAO attributesCacheDAO;
	private final AttributesCachePendingDAO attributesCachePendingDAO;
	private final MembershipDAO membershipDAO;
	private final BulkGroupQueryService bulkGroupQueryService;
	private final AttributesHelper attributesHelper;

	@Autowired
	EffectiveAttributesCacheServiceImpl(AttributesCacheDAO attributesCacheDAO,
			AttributesCachePendingDAO attributesCachePendingDAO, MembershipDAO membershipDAO,
			BulkGroupQueryService bulkGroupQueryService, AttributesHelper attributesHelper)
	{
		this.attributesCacheDAO = attributesCacheDAO;
		this.attributesCachePendingDAO = attributesCachePendingDAO;
		this.membershipDAO = membershipDAO;
		this.bulkGroupQueryService = bulkGroupQueryService;
		this.attributesHelper = attributesHelper;
	}

	@Override
	@Transactional
	public Map<String, AttributeExt> getAttributes(long entityId, String group) throws EngineException
	{
		if (attributesCachePendingDAO.isPending(entityId, group))
		{
			log.trace("consistent getAttributes: entity {} in group {} is pending, computing live", entityId,
					group);
			return attributesHelper.getAllAttributesAsMapOneGroup(entityId, group);
		}
		log.trace("consistent getAttributes: entity {} in group {} is fresh, reading from cache", entityId, group);
		return toMap(attributesCacheDAO.getEntityAttributes(entityId, group));
	}

	@Override
	@Transactional
	public Map<Long, Map<String, AttributeExt>> getGroupAttributes(String group) throws EngineException
	{
		List<EntityInGroup> pendingInGroup = attributesCachePendingDAO.getPendingForGroup(group);
		if (!pendingInGroup.isEmpty())
		{
			log.trace("consistent getGroupAttributes: group {} has {} pending entities, computing live", group,
					pendingInGroup.size());
			GroupMembershipData data = bulkGroupQueryService.getBulkMembershipData(group);
			return bulkGroupQueryService.getGroupUsersAttributes(group, data);
		}
		log.trace("consistent getGroupAttributes: group {} is fully fresh, reading from cache", group);
		return StoredAttributesGrouping.byEntity(attributesCacheDAO.getGroupAttributes(group));
	}

	@Override
	@Transactional
	public CachedAttributes getAttributesFast(long entityId, String group)
	{
		Map<String, AttributeExt> attributes = toMap(attributesCacheDAO.getEntityAttributes(entityId, group));
		boolean pending = attributesCachePendingDAO.isPending(entityId, group);
		log.trace("fast getAttributes: entity {} in group {}: {} cached attributes, pending={}", entityId, group,
				attributes.size(), pending);
		return new CachedAttributes(attributes, pending);
	}

	@Override
	@Transactional
	public Map<Long, CachedAttributes> getGroupAttributesFast(String group)
	{
		Map<Long, Map<String, AttributeExt>> byEntity = StoredAttributesGrouping.byEntity(attributesCacheDAO.getGroupAttributes(group));
		Set<Long> pendingEntities = attributesCachePendingDAO.getPendingForGroup(group).stream()
				.map(EntityInGroup::entityId)
				.collect(Collectors.toSet());

		Map<Long, CachedAttributes> result = new HashMap<>();
		for (GroupMembership member : membershipDAO.getMembers(group))
		{
			long entityId = member.getEntityId();
			Map<String, AttributeExt> attributes = byEntity.getOrDefault(entityId, Collections.emptyMap());
			result.put(entityId, new CachedAttributes(attributes, pendingEntities.contains(entityId)));
		}
		log.trace("fast getGroupAttributes: group {}: {} members, {} pending", group, result.size(),
				pendingEntities.size());
		return result;
	}

	private Map<String, AttributeExt> toMap(List<StoredAttribute> attributes)
	{
		return attributes.stream()
				.collect(Collectors.toMap(a -> a.getAttribute().getName(), StoredAttribute::getAttribute));
	}

}
