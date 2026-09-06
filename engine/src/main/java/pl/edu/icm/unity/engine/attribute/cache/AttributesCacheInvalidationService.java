/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributesCacheInvalidation;
import pl.edu.icm.unity.engine.api.attributes.AttributesCacheRefreshTrigger;
import pl.edu.icm.unity.store.api.AttributesCachePendingDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.tx.TxManager;

@Component
class AttributesCacheInvalidationService implements AttributesCacheInvalidation
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, AttributesCacheInvalidationService.class);

	private final AttributesCachePendingDAO pendingDAO;
	private final MembershipDAO membershipDAO;
	private final TxManager txMan;
	private final AttributesCacheRefreshTrigger refreshTrigger;

	@Autowired
	AttributesCacheInvalidationService(AttributesCachePendingDAO pendingDAO, MembershipDAO membershipDAO,
			TxManager txMan, AttributesCacheRefreshTrigger refreshTrigger)
	{
		this.pendingDAO = pendingDAO;
		this.membershipDAO = membershipDAO;
		this.txMan = txMan;
		this.refreshTrigger = refreshTrigger;
	}

	@Override
	public void invalidateEntity(long entityId)
	{
		Set<String> groups = membershipDAO.getEntityMembershipSimple(entityId);
		log.debug("Invalidating attributes cache for entity {} in all its groups: {}", entityId, groups);
		for (String group : groups)
			pendingDAO.markPending(entityId, group);
		scheduleWakeup();
	}

	@Override
	public void invalidateEntityInGroup(long entityId, String group)
	{
		log.debug("Invalidating attributes cache for entity {} in group {}", entityId, group);
		pendingDAO.markPending(entityId, group);
		scheduleWakeup();
	}

	@Override
	public void invalidateGroup(String group)
	{
		log.debug("Invalidating attributes cache for all members of group {}", group);
		pendingDAO.markPendingForGroup(group);
		scheduleWakeup();
	}

	@Override
	public void invalidateEverything()
	{
		log.debug("Invalidating attributes cache for all entities in all groups (system-wide)");
		int count = 0;
		for (GroupMembership membership : membershipDAO.getAll())
		{
			pendingDAO.markPending(membership.getEntityId(), membership.getGroup());
			count++;
		}
		log.debug("Marked {} (entity, group) pairs as pending", count);
		scheduleWakeup();
	}

	private void scheduleWakeup()
	{
		log.trace("Scheduling attributes cache refresh wakeup as a post-commit action");
		txMan.addPostCommitAction(() ->
		{
			log.trace("Post-commit: waking up attributes cache refresh thread");
			refreshTrigger.wakeUp();
		});
	}
}
