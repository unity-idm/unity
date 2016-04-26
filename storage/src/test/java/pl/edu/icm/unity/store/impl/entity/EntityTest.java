/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.entity;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.store.AbstractBasicDAOTest;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.StoredEntity;
import pl.edu.icm.unity.types.EntityInformation;
import pl.edu.icm.unity.types.EntityScheduledOperation;
import pl.edu.icm.unity.types.EntityState;

public class EntityTest extends AbstractBasicDAOTest<StoredEntity>
{
	@Autowired
	private EntityDAO dao;
	
	@Override
	protected EntityDAO getDAO()
	{
		return dao;
	}

	@Override
	protected StoredEntity getObject(String id)
	{
		EntityInformation ei = new EntityInformation(EntityState.valid);
		ei.setRemovalByUserTime(new Date(2000));
		ei.setScheduledOperation(EntityScheduledOperation.DISABLE);
		ei.setScheduledOperationTime(new Date(1000));
		return new StoredEntity(123l, ei);
	}

	@Override
	protected void mutateObject(StoredEntity src)
	{
		EntityInformation ei = new EntityInformation(EntityState.authenticationDisabled);
		ei.setRemovalByUserTime(new Date(3000));
		ei.setScheduledOperation(EntityScheduledOperation.REMOVE);
		ei.setScheduledOperationTime(new Date(5000));
		src.setEntityInformation(ei);
	}

	@Override
	protected void assertAreEqual(StoredEntity obj, StoredEntity cmp)
	{
		assertThat(obj, is(cmp));
	}
	
	@Test
	public void insertedWithIdIsReturned()
	{
		tx.runInTransaction(() -> {
			StoredEntity obj = getObject("name1");
			long key = obj.getId();
			dao.createWithId(obj);

			StoredEntity ret = dao.getByKey(key);

			assertThat(obj.getId(), is(key));
			assertThat(ret, is(notNullValue()));
			assertAreEqual(obj, ret);
		});
	}

	@Test
	public void regularInsertAfterInsertedWithIdSucceeds()
	{
		tx.runInTransaction(() -> {
			StoredEntity obj = getObject("name1");
			long key = obj.getId();
			dao.createWithId(obj);

			StoredEntity obj2 = getObject("");
			dao.create(obj2);
			
			assertThat(obj2.getId() != key, is(true));
		});
	}
}
