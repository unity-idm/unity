/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributesCacheRefreshTrigger;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.store.api.AttributesCacheDAO;
import pl.edu.icm.unity.store.api.AttributesCachePendingDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

/**
 * Owns and starts the background thread which regenerates the materialized attributes cache for
 * (entity, group) pairs marked as having a pending update ({@link AttributesCachePendingDAO}). Also
 * responsible for satisfying the "regenerate pending attributes on server startup" requirement: since
 * the background thread is started as soon as this component is created, its very first cycle already
 * picks up anything pending at startup (in particular the initial DB migration marking).
 */
@Component
class AttributesCacheRefreshService implements AttributesCacheRefreshTrigger
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, AttributesCacheRefreshService.class);

	private final AttributesCacheRefreshThread refreshThread;

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
		this.refreshThread = new AttributesCacheRefreshThread(bulkGroupQueryService, attributesCacheDAO,
				attributesCachePendingDAO, tx, executorsService.getExecutionService());
		log.debug("Starting attributes cache refresh background thread");
		this.refreshThread.start();
	}

	@Override
	public void wakeUp()
	{
		log.trace("wakeUp() requested via AttributesCacheRefreshTrigger");
		refreshThread.wakeUp();
	}
}
