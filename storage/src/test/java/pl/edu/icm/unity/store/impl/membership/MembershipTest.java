/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.membership;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupMembership;

@RunWith(SpringJUnit4ClassRunner.class)
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
	
	@Before
	public void cleanDB()
	{
		dbCleaner.cleanOrDelete();
		createReferenced();
	}

	@After
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
			assertThat(entityMembership.size(), is(1));
			GroupMembership aM = entityMembership.get(0); 
			
			assertThat(aM, is(gm));
		});
	}	

	@Test
	public void shouldReturnCreatedMembershipByGroup()
	{
		tx.runInTransaction(() -> {
			dao.create(new GroupMembership("/A", entity, null));
			
			List<GroupMembership> entityMembership = dao.getMembers("/A");
			assertThat(entityMembership.size(), is(1));

			assertThat(entityMembership.get(0).getGroup(), is("/A"));
			assertThat(entityMembership.get(0).getEntityId(), is(entity));
		});
	}	

	@Test
	public void shouldReturnCreatedMembershipByGetAll()
	{
		tx.runInTransaction(() -> {
			dao.create(new GroupMembership("/A", entity, null));
			
			List<GroupMembership> entityMembership = dao.getAll();
			assertThat(entityMembership.size(), is(1));

			assertThat(entityMembership.get(0).getGroup(), is("/A"));
			assertThat(entityMembership.get(0).getEntityId(), is(entity));
		});
	}	
	
	@Test
	public void shouldReturnMemberStatusForAddedMembership()
	{
		tx.runInTransaction(() -> {
			dao.create(new GroupMembership("/A", entity, null));
			
			boolean membership = dao.isMember(entity, "/A");
			assertThat(membership, is(true));
		});
	}	

	@Test
	public void shouldNotReturnMemberStatusForMissingMembershipByGroup()
	{
		tx.runInTransaction(() -> {
			dao.create(new GroupMembership("/A", entity, null));
			
			boolean membership = dao.isMember(entity, "/B");
			assertThat(membership, is(false));
		});
	}	

	@Test
	public void shouldNotReturnMemberStatusForMissingMembershipByEntity()
	{
		tx.runInTransaction(() -> {
			dao.create(new GroupMembership("/A", entity, null));
			
			boolean membership = dao.isMember(entity2, "/A");
			assertThat(membership, is(false));
		});
	}	

	@Test
	public void shouldFailOnRemovingMissingMembership()
	{
		tx.runInTransaction(() -> {
			catchException(dao).deleteByKey(entity, "/A");

			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}	
	
	@Test
	public void shouldNotReturnRemovedMembershipByEntity()
	{
		tx.runInTransaction(() -> {
			dao.create(new GroupMembership("/A", entity, null));
			dao.deleteByKey(entity, "/A");
			
			List<GroupMembership> entityMembership = dao.getEntityMembership(entity);
			assertThat(entityMembership.isEmpty(), is(true));
		});
	}	
	
	@Test
	public void shouldNotReturnRemovedMembershipByGroup()
	{
		tx.runInTransaction(() -> {
			dao.create(new GroupMembership("/A", entity, null));
			dao.deleteByKey(entity, "/A");
			
			List<GroupMembership> entityMembership = dao.getMembers("/A");
			assertThat(entityMembership.isEmpty(), is(true));
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
			assertThat(entityMembership.isEmpty(), is(true));
		});
	}

	@Test
	public void shouldNotReturnMembershipForRemovedEntity()
	{
		tx.runInTransaction(() -> {
			dao.create(new GroupMembership("/A", entity, null));
			entDao.deleteByKey(entity);
			
			List<GroupMembership> entityMembership = dao.getMembers("/A");
			assertThat(entityMembership.isEmpty(), is(true));
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
			assertThat(groupMembersA.isEmpty(), is(true));
			
			List<GroupMembership> entityMembership = dao.getEntityMembership(entity);
			assertThat(entityMembership.size(), is(1));
			GroupMembership aM = entityMembership.get(0); 
			assertThat(aM.getGroup(), is("/ZZ"));

			List<GroupMembership> groupMembers = dao.getMembers("/ZZ");
			assertThat(groupMembers.size(), is(1));
			assertThat(groupMembers.get(0).getGroup(), is("/ZZ"));
			assertThat(groupMembers.get(0).getEntityId(), is(entity));
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

			assertThat(ret, is(notNullValue()));
			assertThat(ret.size(), is(2));
		});
	}
}
