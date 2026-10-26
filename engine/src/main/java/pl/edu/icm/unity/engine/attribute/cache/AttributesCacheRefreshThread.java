/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipData;
import pl.edu.icm.unity.store.api.AttributesCacheDAO;
import pl.edu.icm.unity.store.api.AttributesCachePendingDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.types.PendingAttributesCacheEntry;
import pl.edu.icm.unity.store.types.StoredAttribute;

/**
 * Background loop regenerating the materialized attributes cache for all (entity, group) pairs
 * currently marked as having a pending update.
 * <p>
 * Effective attributes are computed outside of the transaction which stores them, so a state change can
 * be committed in between. Therefore the pending flag is cleared only if its version is still the one read
 * before the computation - otherwise it stays set and the entity is refreshed again in the next cycle.
 */
class AttributesCacheRefreshThread extends Thread
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, AttributesCacheRefreshThread.class);
	static final long INTERVAL = 30000;

	private final BulkGroupQueryService bulkGroupQueryService;
	private final AttributesCacheDAO attributesCacheDAO;
	private final AttributesCachePendingDAO attributesCachePendingDAO;
	private final TransactionalRunner tx;
	private final ExecutorService executorService;
	private boolean wakeupRequested = false;
	private volatile boolean stopRequested = false;

	AttributesCacheRefreshThread(BulkGroupQueryService bulkGroupQueryService,
			AttributesCacheDAO attributesCacheDAO,
			AttributesCachePendingDAO attributesCachePendingDAO,
			TransactionalRunner tx,
			ExecutorService executorService)
	{
		super("AttributesCacheRefresh");
		setDaemon(true);
		this.bulkGroupQueryService = bulkGroupQueryService;
		this.attributesCacheDAO = attributesCacheDAO;
		this.attributesCachePendingDAO = attributesCachePendingDAO;
		this.tx = tx;
		this.executorService = executorService;
	}

	@Override
	public void run()
	{
		while (!stopRequested)
		{
			boolean triggeredByWakeup;
			synchronized (this)
			{
				// if a wakeUp() arrived while the previous cycle was still running (i.e. missed by
				// wait() below), do not wait again - process it right away instead of losing it
				// until the next INTERVAL fallback.
				if (!wakeupRequested && !stopRequested)
				{
					log.trace("Refresh thread waiting up to {} ms for a wakeup or the fallback interval", INTERVAL);
					try
					{
						wait(INTERVAL);
					} catch (InterruptedException e)
					{
						Thread.currentThread().interrupt();
						log.debug("Refresh thread interrupted, finishing");
						return;
					}
				}
				triggeredByWakeup = wakeupRequested;
				wakeupRequested = false;
			}
			if (stopRequested)
				break;
			log.trace("Refresh thread starting a cycle (triggered by {})",
					triggeredByWakeup ? "wakeUp()" : "fallback interval");
			try
			{
				refreshAllPending();
			} catch (Exception e)
			{
				log.error("Attributes cache refresh cycle failed", e);
			}
		}
		log.debug("Refresh thread stopped");
	}

	/**
	 * Asks the thread to finish. A cycle in progress is cut short after the entity being currently stored;
	 * entities left unprocessed keep their pending flags. The thread is not interrupted, as interrupting
	 * a thread during DB I/O may break the DB connection (e.g. H2 closes its file on interrupt).
	 */
	public synchronized void requestStop()
	{
		stopRequested = true;
		notify();
	}

	public synchronized void wakeUp()
	{
		log.trace("wakeUp() called (wakeupRequested was already {})", wakeupRequested);
		wakeupRequested = true;
		notify();
	}

	void refreshAllPending()
	{
		List<PendingAttributesCacheEntry> pending = tx.runInTransactionRet(attributesCachePendingDAO::getAll);
		if (pending.isEmpty())
		{
			log.trace("Refresh cycle: nothing pending");
			return;
		}

		Map<String, List<PendingAttributesCacheEntry>> pendingByGroup = pending.stream()
				.collect(Collectors.groupingBy(PendingAttributesCacheEntry::group));
		if (log.isDebugEnabled())
			log.debug("Refresh cycle: {} entities pending, by group: {}", pending.size(),
					pendingByGroup.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
							e -> e.getValue().stream().map(PendingAttributesCacheEntry::entityId).toList())));

		List<Future<?>> futures = new ArrayList<>();
		for (Map.Entry<String, List<PendingAttributesCacheEntry>> entry : pendingByGroup.entrySet())
			futures.add(executorService.submit(() -> refreshGroup(entry.getKey(), entry.getValue())));

		for (Future<?> future : futures)
		{
			try
			{
				future.get();
			} catch (Exception e)
			{
				log.error("Attributes cache refresh task failed", e);
			}
		}
		log.debug("Refresh cycle: all {} groups processed", pendingByGroup.size());
	}

	private void refreshGroup(String group, List<PendingAttributesCacheEntry> pendingEntries)
	{
		if (stopRequested)
			return;
		log.trace("Refreshing group {} for {} pending entities: {}", group, pendingEntries.size(),
				pendingEntries);
		Map<Long, Map<String, AttributeExt>> attributesByEntity;
		try
		{
			GroupMembershipData data = bulkGroupQueryService.getBulkMembershipData(group);
			attributesByEntity = bulkGroupQueryService.getGroupUsersAttributes(group, data);
			log.trace("Group {}: bulk-computed effective attributes for {} entities", group,
					attributesByEntity.size());
		} catch (EngineException | RuntimeException e)
		{
			log.error("Failed to load bulk attributes data for group {}, will retry on next cycle", group, e);
			return;
		}

		for (PendingAttributesCacheEntry entry : pendingEntries)
		{
			if (stopRequested)
			{
				log.debug("Group {} refresh aborted due to stop request", group);
				return;
			}
			refreshEntity(entry, attributesByEntity.get(entry.entityId()));
		}
		log.trace("Group {} refresh finished", group);
	}

	private void refreshEntity(PendingAttributesCacheEntry pendingEntry, Map<String, AttributeExt> effectiveAttributes)
	{
		long entityId = pendingEntry.entityId();
		String group = pendingEntry.group();
		boolean stillMember = effectiveAttributes != null;
		Map<String, AttributeExt> attributes = effectiveAttributes == null
				? Collections.emptyMap()
				: CacheableAttributes.of(effectiveAttributes);
		log.trace("Refreshing entity {} in group {}: {} attributes, still a member: {}", entityId, group,
				attributes.size(), stillMember);
		try
		{
			boolean cleared = tx.runInTransactionRet(() ->
			{
				attributesCacheDAO.deleteCacheInGroup(entityId, group);
				List<StoredAttribute> toStore = attributes.values().stream()
						.map(a ->
						{
							AttributeExt copy = new AttributeExt(a);
							copy.setGroupPath(group);
							return new StoredAttribute(copy, entityId);
						})
						.collect(Collectors.toList());
				attributesCacheDAO.createList(toStore);
				return attributesCachePendingDAO.clearPendingIfVersion(entityId, group, pendingEntry.version());
			});
			if (cleared)
				log.trace("Entity {} in group {}: cache updated and pending flag cleared", entityId, group);
			else
				log.debug("Entity {} in group {}: cache updated, but it was invalidated again during the refresh "
						+ "- pending flag kept for the next cycle", entityId, group);
		} catch (Exception e)
		{
			log.error("Failed to refresh attributes cache for entity {} in group {}", entityId, group, e);
		}
	}
}
