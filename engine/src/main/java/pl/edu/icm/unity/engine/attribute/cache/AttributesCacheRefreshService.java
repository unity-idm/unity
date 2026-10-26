/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributesCacheRefreshTrigger;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.store.api.AttributesCacheDAO;
import pl.edu.icm.unity.store.api.AttributesCachePendingDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

/**
 * Owns the background thread which regenerates the materialized attributes cache for (entity, group) pairs
 * marked as having a pending update ({@link AttributesCachePendingDAO}). The thread lifecycle follows the
 * Spring context: started after the engine initialization and stopped (awaiting its termination) before
 * the context's beans are destroyed. Also responsible for satisfying the "regenerate pending attributes on
 * server startup" requirement: the very first cycle of the thread picks up anything pending at startup
 * (in particular the initial DB migration marking).
 */
@Component
class AttributesCacheRefreshService implements AttributesCacheRefreshTrigger, SmartLifecycle
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, AttributesCacheRefreshService.class);
	static final long STOP_TIMEOUT_MS = 30000;

	private final BulkGroupQueryService bulkGroupQueryService;
	private final AttributesCacheDAO attributesCacheDAO;
	private final AttributesCachePendingDAO attributesCachePendingDAO;
	private final TransactionalRunner tx;
	private final ExecutorsService executorsService;
	volatile AttributesCacheRefreshThread refreshThread;

	@Autowired
	AttributesCacheRefreshService(
			// "insecure" variant: this background thread never runs within a real invocation
			// context (no HTTP request/session), so the normal authz-checked BulkGroupQueryService
			// would fail with "no invocation context set". Same pattern as e.g. ExistingUserFinder.
			@Qualifier("insecure") BulkGroupQueryService bulkGroupQueryService,
			AttributesCacheDAO attributesCacheDAO,
			AttributesCachePendingDAO attributesCachePendingDAO,
			TransactionalRunner tx,
			ExecutorsService executorsService)
	{
		this.bulkGroupQueryService = bulkGroupQueryService;
		this.attributesCacheDAO = attributesCacheDAO;
		this.attributesCachePendingDAO = attributesCachePendingDAO;
		this.tx = tx;
		this.executorsService = executorsService;
	}

	@Override
	public synchronized void start()
	{
		if (refreshThread != null)
			return;
		log.debug("Starting attributes cache refresh background thread");
		refreshThread = new AttributesCacheRefreshThread(bulkGroupQueryService, attributesCacheDAO,
				attributesCachePendingDAO, tx, executorsService.getExecutionService());
		refreshThread.start();
	}

	@Override
	public synchronized void stop()
	{
		AttributesCacheRefreshThread thread = refreshThread;
		if (thread == null)
			return;
		refreshThread = null;
		log.debug("Stopping attributes cache refresh background thread");
		thread.requestStop();
		try
		{
			thread.join(STOP_TIMEOUT_MS);
		} catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
		if (thread.isAlive())
			log.warn("Attributes cache refresh thread did not finish within {} ms", STOP_TIMEOUT_MS);
	}

	@Override
	public boolean isRunning()
	{
		return refreshThread != null;
	}

	@Override
	public int getPhase()
	{
		return EngineInitialization.ENGINE_INITIALIZATION_MOMENT + 1;
	}

	@Override
	public void wakeUp()
	{
		AttributesCacheRefreshThread thread = refreshThread;
		if (thread == null)
		{
			log.trace("wakeUp() ignored, refresh thread is not running");
			return;
		}
		log.trace("wakeUp() requested via AttributesCacheRefreshTrigger");
		thread.wakeUp();
	}
}
