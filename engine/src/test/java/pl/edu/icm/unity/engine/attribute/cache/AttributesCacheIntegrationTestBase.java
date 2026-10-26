/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.attributes.CachedAttributes;
import pl.edu.icm.unity.engine.api.attributes.EffectiveAttributesCacheService;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.store.api.AttributesCacheDAO;
import pl.edu.icm.unity.store.api.AttributesCachePendingDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.types.EntityInGroup;

/**
 * Base of attributes cache integration tests. The background refresh is stopped for each test, so that the
 * pending flags are deterministic; a test can run it with {@link #startBackgroundRefresh()}.
 */
abstract class AttributesCacheIntegrationTestBase extends DBIntegrationTestBase
{
	@Autowired
	protected AttributesCacheRefreshService refreshService;
	@Autowired
	protected AttributesCachePendingDAO attributesCachePendingDAO;
	@Autowired
	protected AttributesCacheDAO attributesCacheDAO;
	@Autowired
	protected EffectiveAttributesCacheService effectiveAttributesCache;
	@Autowired
	protected TransactionalRunner tx;

	@BeforeEach
	public void stopBackgroundRefresh()
	{
		refreshService.stop();
	}

	@AfterEach
	public void restartBackgroundRefresh()
	{
		refreshService.start();
	}

	protected void startBackgroundRefresh()
	{
		refreshService.start();
		refreshService.wakeUp();
	}

	protected long addEntity(String name, String... groups) throws Exception
	{
		long entityId = idsMan.addEntity(new IdentityParam(IdentifierIdentity.ID, name), EntityState.valid)
				.getEntityId();
		for (String group : groups)
			groupsMan.addMemberFromParent(group, new EntityParam(entityId));
		return entityId;
	}

	protected void clearAllPending()
	{
		tx.runInTransaction(() -> attributesCachePendingDAO.getAll().forEach(
				p -> attributesCachePendingDAO.clearPendingIfVersion(p.entityId(), p.group(), p.version())));
	}

	protected Set<EntityInGroup> pendingPairs()
	{
		return tx.runInTransactionRet(() -> attributesCachePendingDAO.getAll().stream()
				.map(p -> pair(p.entityId(), p.group()))
				.collect(Collectors.toSet()));
	}

	protected boolean isPending(long entityId, String group)
	{
		return tx.runInTransactionRet(() -> attributesCachePendingDAO.isPending(entityId, group));
	}

	protected static EntityInGroup pair(long entityId, String group)
	{
		return new EntityInGroup(entityId, group);
	}

	/**
	 * Waits until the background refresh made the cache of the given entity fresh and its value of the
	 * attribute (absent if values == null) as expected.
	 */
	protected void awaitFreshCachedValues(long entityId, String group, String attribute, List<String> values)
	{
		Awaitility.await().pollInSameThread().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
		{
			CachedAttributes cached = effectiveAttributesCache.getAttributesFast(entityId, group);
			assertThat(cached.updatePending()).isFalse();
			AttributeExt attr = cached.attributes().get(attribute);
			if (values == null)
				assertThat(attr).isNull();
			else
				assertThat(attr).isNotNull().extracting(AttributeExt::getValues).isEqualTo(values);
		});
	}
}
