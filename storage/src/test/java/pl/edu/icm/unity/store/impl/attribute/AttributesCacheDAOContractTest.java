/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.AttributesCacheDAO;
import pl.edu.icm.unity.store.api.AttributesCachePendingDAO;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.types.StoredAttribute;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath*:META-INF/components.xml" })
public class AttributesCacheDAOContractTest
{
	@Autowired
	private AttributesCacheDAO dao;
	@Autowired
	private AttributesCachePendingDAO pendingDAO;
	@Autowired
	private GroupDAO groupDao;
	@Autowired
	private EntityDAO entDao;
	@Autowired
	private AttributeTypeDAO atDao;
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
			atDao.create(new AttributeType("attr", "syntax"));
			atDao.create(new AttributeType("attr2", "syntax"));
		});
	}

	@AfterEach
	public void shutdown()
	{
		dbCleaner.shutdown();
	}

	@Test
	public void emptyBatchWriteIsNoop()
	{
		tx.runInTransaction(() ->
		{
			dao.createList(List.of());

			assertThat(dao.getGroupAttributes("/A")).isEmpty();
		});
	}

	@Test
	public void batchWriteStoresAllAttributesWithCompleteMetadata()
	{
		AttributeExt full = new AttributeExt(new Attribute("attr", "syntax", "/A", List.of("v1", "v2"),
				"remoteIdp", "profile"), false, new Date(1000), new Date(2000));
		AttributeExt other = new AttributeExt(new Attribute("attr2", "syntax", "/A", List.of("w")), true);

		tx.runInTransaction(() ->
		{
			dao.createList(List.of(new StoredAttribute(full, entity), new StoredAttribute(other, entity)));

			List<StoredAttribute> read = dao.getEntityAttributes(entity, "/A");
			assertThat(read).hasSize(2);
			AttributeExt readFull = read.stream().map(StoredAttribute::getAttribute)
					.filter(a -> a.getName().equals("attr")).findFirst().orElseThrow();
			assertThat(readFull).isEqualTo(full);
			assertThat(readFull.getValues()).containsExactly("v1", "v2");
			assertThat(readFull.isDirect()).isFalse();
			assertThat(readFull.getRemoteIdp()).isEqualTo("remoteIdp");
			assertThat(readFull.getTranslationProfile()).isEqualTo("profile");
			assertThat(readFull.getCreationTs()).isEqualTo(new Date(1000));
			assertThat(readFull.getUpdateTs()).isEqualTo(new Date(2000));
			assertThat(readFull.getValueSyntax()).isEqualTo("syntax");
			assertThat(readFull.getGroupPath()).isEqualTo("/A");
		});
	}

	@Test
	public void deletionRemovesOnlyTheGivenEntityGroupPair()
	{
		tx.runInTransaction(() ->
		{
			dao.createList(List.of(cached(entity, "/A"), cached(entity, "/B"), cached(entity2, "/A")));

			dao.deleteCacheInGroup(entity, "/A");

			assertThat(dao.getEntityAttributes(entity, "/A")).isEmpty();
			assertThat(dao.getEntityAttributes(entity, "/B")).hasSize(1);
			assertThat(dao.getEntityAttributes(entity2, "/A")).hasSize(1);
		});
	}

	@Test
	public void markingGroupMembersRepeatedlyDoesNotDuplicateFlags()
	{
		tx.runInTransaction(() ->
		{
			membershipDao.create(new GroupMembership("/A", entity, new Date()));

			pendingDAO.markPendingForGroupMembers("/A");
			pendingDAO.markPendingForGroupMembers("/A");
			pendingDAO.markPendingForAll();
			pendingDAO.markPending(entity, "/A");

			assertThat(pendingDAO.getAll()).hasSize(1);
		});
	}

	@Test
	public void entityRemovalCleansUpItsCacheAndPendingFlags()
	{
		tx.runInTransaction(() ->
		{
			dao.createList(List.of(cached(entity, "/A"), cached(entity2, "/A")));
			pendingDAO.markPending(entity, "/A");
			pendingDAO.markPending(entity2, "/A");

			entDao.deleteByKey(entity);

			assertThat(dao.getGroupAttributes("/A")).extracting(StoredAttribute::getEntityId)
					.containsExactly(entity2);
			assertThat(pendingDAO.getAll()).extracting(p -> p.entityId()).containsExactly(entity2);
		});
	}

	@Test
	public void groupRemovalCleansUpItsCacheAndPendingFlags()
	{
		tx.runInTransaction(() ->
		{
			dao.createList(List.of(cached(entity, "/A"), cached(entity, "/B")));
			pendingDAO.markPending(entity, "/A");
			pendingDAO.markPending(entity, "/B");

			groupDao.delete("/B");

			assertThat(dao.getEntityAttributes(entity, "/A")).hasSize(1);
			assertThat(pendingDAO.getAll()).extracting(p -> p.group()).containsExactly("/A");
		});
	}

	@Test
	public void attributeTypeRemovalCleansUpItsCachedValues()
	{
		tx.runInTransaction(() ->
		{
			dao.createList(List.of(cached(entity, "/A"), new StoredAttribute(
					new AttributeExt(new Attribute("attr2", "syntax", "/A", List.of("w")), true), entity)));

			atDao.delete("attr2");

			assertThat(dao.getEntityAttributes(entity, "/A")).extracting(sa -> sa.getAttribute().getName())
					.containsExactly("attr");
		});
	}

	@Test
	public void failedReplacementAfterDeletionIsRolledBack()
	{
		tx.runInTransaction(() -> dao.createList(List.of(cached(entity, "/A"))));

		assertThatThrownBy(() -> tx.runInTransaction(() ->
		{
			dao.deleteCacheInGroup(entity, "/A");
			dao.createList(List.of(new StoredAttribute(
					new AttributeExt(new Attribute("unknownType", "syntax", "/A", List.of("x")), true), entity)));
		})).isInstanceOf(RuntimeException.class);

		tx.runInTransaction(() -> assertThat(dao.getEntityAttributes(entity, "/A"))
				.extracting(sa -> sa.getAttribute().getValues()).containsExactly(List.of("v")));
	}

	private StoredAttribute cached(long entityId, String group)
	{
		return new StoredAttribute(new AttributeExt(new Attribute("attr", "syntax", group, List.of("v")), true),
				entityId);
	}
}
