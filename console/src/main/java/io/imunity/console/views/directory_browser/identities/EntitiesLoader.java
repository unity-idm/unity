/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.google.common.base.Stopwatch;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@PrototypeComponent
class EntitiesLoader
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EntitiesLoader.class);
	private final BulkGroupQueryService bulkQueryService;

	EntitiesLoader(BulkGroupQueryService bulkQueryService)
	{
		this.bulkQueryService = bulkQueryService;
	}

	void reload(Set<IdentityEntry> selected, String group, boolean includeTargeted,
			EntitiesConsumer consumer) throws EngineException
	{
		Stopwatch watch = Stopwatch.createStarted();
		
		GroupMembershipData bulkData = bulkQueryService.getBulkMembershipData(group);
		Map<Long, Entity> groupEntities = includeTargeted ? 
				bulkQueryService.getGroupEntitiesNoContextWithTargeted(bulkData) : 
				bulkQueryService.getGroupEntitiesNoContextWithoutTargeted(bulkData);
		Map<Long, Map<String, AttributeExt>> rootAttributes = bulkQueryService.getGroupUsersAttributes("/", bulkData);
		Map<Long, Map<String, AttributeExt>> groupAttributes = "/".equals(group) ?
				rootAttributes :
				bulkQueryService.getGroupUsersAttributes(group, bulkData);
		
		List<ResolvedEntity> ret = new ArrayList<>(groupEntities.size());
		for (Long entityId: groupEntities.keySet())
		{
			Entity entity = groupEntities.get(entityId);
			ResolvedEntity resolvedEntity = new ResolvedEntity(entity, 
					entity.getIdentities(), 
					rootAttributes.get(entityId), groupAttributes.get(entityId));
			ret.add(resolvedEntity);
		}
		watch.stop();
		log.debug("Resolved {} users in {}, {} users/s", groupEntities.size(), watch.toString(), 
				(1000.0*groupEntities.size()/watch.elapsed(TimeUnit.MILLISECONDS)));
		consumer.consume(ret, selected, 1.0f);
	}

	@FunctionalInterface
	interface EntitiesConsumer
	{
		void consume(List<ResolvedEntity> toAdd, Set<IdentityEntry> selected,
				float progress);
	}
}
