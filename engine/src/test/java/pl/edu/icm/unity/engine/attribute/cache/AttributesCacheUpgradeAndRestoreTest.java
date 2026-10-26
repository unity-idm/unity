/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.json.dump.DBDumpContentElements;
import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.store.api.ImportExport;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.rdbms.InitDB;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;
import pl.edu.icm.unity.store.types.EntityInGroup;

/**
 * Upgrade of a DB from before the attributes cache was introduced (schema 23), and DB restore from dumps,
 * which don't contain the cache.
 */
public class AttributesCacheUpgradeAndRestoreTest extends AttributesCacheIntegrationTestBase
{
	@Autowired
	private InitDB initDB;
	@Autowired
	private ImportExport importExport;
	@Autowired
	private MembershipDAO membershipDAO;
	@Autowired
	private UnityServerConfiguration configuration;

	private long entityId;
	private EntityParam entity;

	@BeforeEach
	public void setup() throws Exception
	{
		aTypeMan.addAttributeType(new AttributeType("a1", StringAttributeSyntax.ID));
		groupsMan.addGroup(new Group("/A"));
		entityId = addEntity("e1", "/A");
		entity = new EntityParam(entityId);
		attrsMan.createAttribute(entity, StringAttribute.of("a1", "/A", "v1"));
		FileUtils.deleteDirectory(new File(
				configuration.getFileValue(UnityServerConfiguration.WORKSPACE_DIRECTORY, true),
				ServerManagement.DB_DUMP_DIRECTORY));
	}

	@Test
	public void upgradeFromSchema23CreatesCacheTablesAndSeedsAllMembershipsForRefresh() throws Exception
	{
		downgradeToSchema23();

		initDB.initIfNeeded();
		initDB.updateContents();

		assertThat(pendingPairs()).isEqualTo(allMemberships());
		startBackgroundRefresh();
		awaitFreshCachedValues(entityId, "/A", "a1", List.of("v1"));
	}

	@Test
	public void restoreOfSchema23DumpSeedsAllMembershipsForRefresh() throws Exception
	{
		File dump = exportWithVersion(23);
		clearAllPending();

		serverMan.importDb(dump);

		assertThat(pendingPairs()).containsAll(allMemberships());
		startBackgroundRefresh();
		awaitFreshCachedValues(entityId, "/A", "a1", List.of("v1"));
	}

	@Test
	public void restoreOverExistingDatabaseDoesNotLeaveObsoleteCacheAsFresh() throws Exception
	{
		File dump = serverMan.exportDb(new DBDumpContentElements());
		// state changed after the dump was made, and already present in the cache
		attrsMan.setAttribute(entity, StringAttribute.of("a1", "/A", "after-dump"));
		long addedAfterDump = addEntity("e-after-dump", "/A");
		attrsMan.createAttribute(new EntityParam(addedAfterDump), StringAttribute.of("a1", "/A", "x"));
		startBackgroundRefresh();
		awaitFreshCachedValues(entityId, "/A", "a1", List.of("after-dump"));
		refreshService.stop();

		serverMan.importDb(dump);

		assertThat(isPending(entityId, "/A")).isTrue();
		assertThat(cachedEntities()).doesNotContain(addedAfterDump);
		startBackgroundRefresh();
		awaitFreshCachedValues(entityId, "/A", "a1", List.of("v1"));
	}

	private void downgradeToSchema23()
	{
		tx.runInTransaction(() ->
		{
			try (Statement statement = SQLTransactionTL.getSql().getConnection().createStatement())
			{
				statement.execute("DROP TABLE ATTRIBUTES_CACHE_PENDING");
				statement.execute("DROP TABLE ATTRIBUTES_CACHE");
				statement.execute("UPDATE UVOS_FLAG SET VAL = '23'");
			} catch (Exception e)
			{
				throw new IllegalStateException(e);
			}
		});
	}

	private File exportWithVersion(int version) throws Exception
	{
		File dump = File.createTempFile("dump-v" + version, ".json");
		dump.deleteOnExit();
		tx.runInTransactionThrowing(() ->
		{
			try (OutputStream os = new BufferedOutputStream(new FileOutputStream(dump)))
			{
				importExport.storeWithVersion(os, new DBDumpContentElements(), version);
			} catch (Exception e)
			{
				throw new IllegalStateException(e);
			}
		});
		return dump;
	}

	private Set<EntityInGroup> allMemberships()
	{
		return tx.runInTransactionRet(() -> membershipDAO.getAll().stream()
				.map(m -> pair(m.getEntityId(), m.getGroup()))
				.collect(Collectors.toSet()));
	}

	private Set<Long> cachedEntities()
	{
		return tx.runInTransactionRet(() -> attributesCacheDAO.getGroupAttributes("/A").stream()
				.map(sa -> sa.getEntityId())
				.collect(Collectors.toSet()));
	}
}
