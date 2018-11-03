/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.entity;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.impl.AbstractBasicDAOTest;
import pl.edu.icm.unity.store.tx.TransactionTL;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.EntityScheduledOperation;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupMembership;

public class EntityTest extends AbstractBasicDAOTest<EntityInformation>
{
	@Autowired
	private EntityDAO dao;
	
	@Autowired
	private MembershipDAO membershipDao;
	
	@Autowired
	private GroupDAO groupDAO;
	
	@Override
	protected EntityDAO getDAO()
	{
		return dao;
	}

	@Override
	protected EntityInformation getObject(String id)
	{
		EntityInformation ei = new EntityInformation();
		ei.setId(123l);
		ei.setRemovalByUserTime(new Date(2000));
		ei.setScheduledOperation(EntityScheduledOperation.DISABLE);
		ei.setScheduledOperationTime(new Date(1000));
		return ei;
	}

	@Override
	protected EntityInformation mutateObject(EntityInformation src)
	{
		src.setEntityState(EntityState.authenticationDisabled);
		src.setRemovalByUserTime(new Date(3000));
		src.setScheduledOperation(EntityScheduledOperation.REMOVE);
		src.setScheduledOperationTime(new Date(5000));
		return src;
	}

	@Test
	public void insertedWithIdIsReturned()
	{
		tx.runInTransaction(() -> {
			EntityInformation obj = getObject("name1");
			long key = obj.getId();
			dao.createWithId(key, obj);

			EntityInformation ret = dao.getByKey(key);

			assertThat(obj.getId(), is(key));
			assertThat(ret, is(notNullValue()));
			assertThat(ret, is(obj));
		});
	}
	
	@Test
	public void uniqueIndexesAreAssigned()
	{
		tx.runInTransaction(() -> {
			long id1 = dao.create(new EntityInformation());
			long id2 = dao.create(new EntityInformation());

			assertThat(id1, is(not(id2)));
			
			TransactionTL.manualCommit();
			
			EntityInformation ret1 = dao.getByKey(id1);
			EntityInformation ret2 = dao.getByKey(id2);

			assertThat(ret1.getId(), is(id1));
			assertThat(ret2.getId(), is(id2));
		});
	}
	
	@Test
	public void regularInsertAfterInsertedWithIdSucceeds()
	{
		tx.runInTransaction(() -> {
			EntityInformation obj = getObject("name1");
			long key = obj.getId();
			dao.createWithId(key, obj);

			EntityInformation obj2 = getObject("");
			dao.create(obj2);
			
			assertThat(obj2.getId() != key, is(true));
		});
	}
	
	@Test
	public void shouldReturnByGroupMembership()
	{
		tx.runInTransaction(() -> {
			long id1 = dao.create(getObject("1"));
			long id2 = dao.create(getObject("2"));
			long id3 = dao.create(getObject("3"));
			
			groupDAO.create(new Group("/C"));
			groupDAO.create(new Group("/A"));
			membershipDao.create(new GroupMembership("/C", id1, new Date(1)));
			membershipDao.create(new GroupMembership("/C", id2, new Date(1)));
			membershipDao.create(new GroupMembership("/A", id3, new Date(1)));
			
			List<EntityInformation> ret = dao.getByGroup("/C");

			assertThat(ret, is(notNullValue()));
			assertThat(ret.size(), is(2));
			assertThat(ret, hasItems(dao.getByKey(id1), dao.getByKey(id2)));
		});
	}
}
