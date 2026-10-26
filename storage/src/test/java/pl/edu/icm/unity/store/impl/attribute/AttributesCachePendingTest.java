/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.AttributesCachePendingDAO;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.types.PendingAttributesCacheEntry;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath*:META-INF/components.xml" })
public class AttributesCachePendingTest
{
	@Autowired
	private AttributesCachePendingDAO dao;
	@Autowired
	private GroupDAO groupDao;
	@Autowired
	private EntityDAO entDao;
	@Autowired
	private MembershipDAO membershipDao;
	@Autowired
	private StorageCleanerImpl dbCleaner;
	@Autowired
	private TransactionalRunner tx;

	private long entity;
	private long entity2;

	@BeforeEach
	public void cleanDB()
	{
		dbCleaner.cleanOrDelete();
		tx.runInTransaction(() ->
		{
			entity = entDao.create(new EntityInformation());
			entity2 = entDao.create(new EntityInformation());
			groupDao.create(new Group("/A"));
			groupDao.create(new Group("/B"));
		});
	}

	@AfterEach
	public void shutdown()
	{
		dbCleaner.shutdown();
	}

	@Test
	public void shouldBumpVersionWhenMarkingAlreadyPending()
	{
		tx.runInTransaction(() ->
		{
			dao.markPending(entity, "/A");
			long initial = getVersion(entity);

			dao.markPending(entity, "/A");

			assertThat(dao.getAll()).hasSize(1);
			assertThat(getVersion(entity)).isGreaterThan(initial);
		});
	}

	@Test
	public void shouldClearPendingWithMatchingVersion()
	{
		tx.runInTransaction(() ->
		{
			dao.markPending(entity, "/A");

			boolean cleared = dao.clearPendingIfVersion(entity, "/A", getVersion(entity));

			assertThat(cleared).isTrue();
			assertThat(dao.isPending(entity, "/A")).isFalse();
		});
	}

	@Test
	public void shouldNotClearPendingMarkedAgainAfterVersionWasRead()
	{
		tx.runInTransaction(() ->
		{
			dao.markPending(entity, "/A");
			long processedVersion = getVersion(entity);
			dao.markPending(entity, "/A");

			boolean cleared = dao.clearPendingIfVersion(entity, "/A", processedVersion);

			assertThat(cleared).isFalse();
			assertThat(dao.isPending(entity, "/A")).isTrue();
		});
	}

	@Test
	public void shouldMarkGroupMembersInAllTheirGroups()
	{
		tx.runInTransaction(() ->
		{
			membershipDao.create(new GroupMembership("/A", entity, new Date()));
			membershipDao.create(new GroupMembership("/B", entity, new Date()));
			membershipDao.create(new GroupMembership("/B", entity2, new Date()));

			dao.markPendingForGroupMembers("/A");

			assertThat(dao.isPending(entity, "/A")).isTrue();
			assertThat(dao.isPending(entity, "/B")).isTrue();
			assertThat(dao.isPending(entity2, "/B")).isFalse();
			assertThat(dao.getAll()).hasSize(2);
		});
	}

	@Test
	public void shouldBumpVersionOfAlreadyPendingWhenMarkingGroupMembers()
	{
		tx.runInTransaction(() ->
		{
			membershipDao.create(new GroupMembership("/A", entity, new Date()));
			membershipDao.create(new GroupMembership("/B", entity, new Date()));
			membershipDao.create(new GroupMembership("/B", entity2, new Date()));
			dao.markPending(entity, "/B");
			dao.markPending(entity2, "/B");
			long initial = getVersion(entity, "/B");
			long initialOfNonMember = getVersion(entity2, "/B");

			dao.markPendingForGroupMembers("/A");

			assertThat(getVersion(entity, "/B")).isGreaterThan(initial);
			assertThat(getVersion(entity2, "/B")).isEqualTo(initialOfNonMember);
		});
	}

	@Test
	public void shouldMarkAllMembershipsAndBumpVersionOfAlreadyPending()
	{
		tx.runInTransaction(() ->
		{
			membershipDao.create(new GroupMembership("/A", entity, new Date()));
			membershipDao.create(new GroupMembership("/B", entity, new Date()));
			membershipDao.create(new GroupMembership("/B", entity2, new Date()));
			dao.markPending(entity, "/A");
			long initial = getVersion(entity, "/A");

			dao.markPendingForAll();

			assertThat(dao.getAll()).hasSize(3);
			assertThat(dao.isPending(entity, "/B")).isTrue();
			assertThat(dao.isPending(entity2, "/B")).isTrue();
			assertThat(getVersion(entity, "/A")).isGreaterThan(initial);
		});
	}

	private long getVersion(long entityId)
	{
		return getVersion(entityId, "/A");
	}

	private long getVersion(long entityId, String group)
	{
		return dao.getAll().stream()
				.filter(e -> e.entityId() == entityId && e.group().equals(group))
				.findFirst()
				.map(PendingAttributesCacheEntry::version)
				.orElseThrow();
	}
}
