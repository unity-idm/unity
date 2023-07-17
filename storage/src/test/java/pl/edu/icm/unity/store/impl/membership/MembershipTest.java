/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
public class MembershipTest
{
	@Autowired
	private MembershipDAO dao;

	@Autowired
	private GroupDAO groupDao;

	@Autowired
	private EntityDAO entDao;

	@Autowired
	private StorageCleanerImpl dbCleaner;

	@Autowired
	protected TransactionalRunner tx;

	private long entity;

	private long entity2;
	
	@BeforeEach
	public void cleanDB()
	{
		dbCleaner.cleanOrDelete();
		createReferenced();
	}

	@AfterEach
	public void shutdown()
	{
		dbCleaner.shutdown();
	}
	
	private void createReferenced()
	{
		tx.runInTransaction(() -> {
			entity = entDao.create(new EntityInformation());
			entity2 = entDao.create(new EntityInformation());
			groupDao.create(new Group("/A"));
			groupDao.create(new Group("/B"));
		});
	}
	
	@Test
	public void shouldReturnCreatedMembershipByEntity()
	{
		tx.runInTransaction(() -> {
			Date now = new Date();
			GroupMembership gm = new GroupMembership("/A", entity, now);
			gm.setRemoteIdp("idp");
			gm.setTranslationProfile("tp");
			dao.create(gm);
			
			List<GroupMembership> entityMembership = dao.getEntityMembership(entity);
			assertThat(entityMembership).hasSize(1);
			GroupMembership aM = entityMembership.get(0); 
			
			assertThat(aM).isEqualTo(gm);
		});
	}	

	@Test
	public void shouldReturnCreatedMembershipByGroup()
	{
		tx.runInTransaction(() -> {
			dao.create(new GroupMembership("/A", entity, null));
			
			List<GroupMembership> entityMembership = dao.getMembers("/A");
			assertThat(entityMembership).hasSize(1);

			assertThat(entityMembership.get(0).getGroup()).isEqualTo("/A");
			assertThat(entityMembership.get(0).getEntityId()).isEqualTo(entity);
		});
	}	

	@Test
	public void shouldReturnCreatedMembershipByGetAll()
	{
		tx.runInTransaction(() -> {
			dao.create(new GroupMembership("/A", entity, null));
			
			List<GroupMembership> entityMembership = dao.getAll();
			assertThat(entityMembership).hasSize(1);

			assertThat(entityMembership.get(0).getGroup()).isEqualTo("/A");
			assertThat(entityMembership.get(0).getEntityId()).isEqualTo(entity);
		});
	}	
	
	@Test
	public void shouldReturnMemberStatusForAddedMembership()
	{
		tx.runInTransaction(() -> {
			dao.create(new GroupMembership("/A", entity, null));
			
			boolean membership = dao.isMember(entity, "/A");
			assertThat(membership).isTrue();
		});
	}	

	@Test
	public void shouldNotReturnMemberStatusForMissingMembershipByGroup()
	{
		tx.runInTransaction(() -> {
			dao.create(new GroupMembership("/A", entity, null));
			
			boolean membership = dao.isMember(entity, "/B");
			assertThat(membership).isFalse();
		});
	}	

	@Test
	public void shouldNotReturnMemberStatusForMissingMembershipByEntity()
	{
		tx.runInTransaction(() -> {
			dao.create(new GroupMembership("/A", entity, null));
			
			boolean membership = dao.isMember(entity2, "/A");
			assertThat(membership).isFalse();
		});
	}	

	@Test
	public void shouldFailOnRemovingMissingMembership()
	{
		tx.runInTransaction(() -> {
			Throwable error = catchThrowable(() -> dao.deleteByKey(entity, "/A"));

			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}	
	
	@Test
	public void shouldNotReturnRemovedMembershipByEntity()
	{
		tx.runInTransaction(() -> {
			dao.create(new GroupMembership("/A", entity, null));
			dao.deleteByKey(entity, "/A");
			
			List<GroupMembership> entityMembership = dao.getEntityMembership(entity);
			assertThat(entityMembership.isEmpty()).isTrue();
		});
	}	
	
	@Test
	public void shouldNotReturnRemovedMembershipByGroup()
	{
		tx.runInTransaction(() -> {
			dao.create(new GroupMembership("/A", entity, null));
			dao.deleteByKey(entity, "/A");
			
			List<GroupMembership> entityMembership = dao.getMembers("/A");
			assertThat(entityMembership.isEmpty()).isTrue();
		});
	}
	
	@Test
	public void shouldNotReturnMembershipInRemovedGroup()
	{
		tx.runInTransaction(() -> {
			dao.create(new GroupMembership("/A", entity, null));
			groupDao.delete("/A");
			groupDao.create(new Group("/A"));
			
			List<GroupMembership> entityMembership = dao.getMembers("/A");
			assertThat(entityMembership.isEmpty()).isTrue();
		});
	}

	@Test
	public void shouldNotReturnMembershipForRemovedEntity()
	{
		tx.runInTransaction(() -> {
			dao.create(new GroupMembership("/A", entity, null));
			entDao.deleteByKey(entity);
			
			List<GroupMembership> entityMembership = dao.getMembers("/A");
			assertThat(entityMembership.isEmpty()).isTrue();
		});
	}

	@Test
	public void shouldRenameMembershipGroupPathsAfterGroupRename()
	{
		tx.runInTransaction(() -> {
			dao.create(new GroupMembership("/A", entity, null));
			long groupKey = groupDao.getKeyForName("/A");
			Group renamed = new Group("/ZZ");
			groupDao.updateByKey(groupKey, renamed);
			groupDao.create(new Group("/A"));
			
			List<GroupMembership> groupMembersA = dao.getMembers("/A");
			assertThat(groupMembersA.isEmpty()).isTrue();
			
			List<GroupMembership> entityMembership = dao.getEntityMembership(entity);
			assertThat(entityMembership).hasSize(1);
			GroupMembership aM = entityMembership.get(0); 
			assertThat(aM.getGroup()).isEqualTo("/ZZ");

			List<GroupMembership> groupMembers = dao.getMembers("/ZZ");
			assertThat(groupMembers).hasSize(1);
			assertThat(groupMembers.get(0).getGroup()).isEqualTo("/ZZ");
			assertThat(groupMembers.get(0).getEntityId()).isEqualTo(entity);
		});
	}
	
	@Test
	public void insertedListIsReturned()
	{
		tx.runInTransaction(() -> {
			Date now = new Date();
			GroupMembership gm1 = new GroupMembership("/A", entity, now);
			GroupMembership gm2 = new GroupMembership("/B", entity2, now);
			
			dao.createList(Lists.newArrayList(gm1, gm2));

			List<GroupMembership> ret = dao.getAll();

			assertThat(ret).isNotNull();
			assertThat(ret).hasSize(2);
		});
	}
}
