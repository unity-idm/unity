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

import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.impl.AbstractBasicDAOTest;
import pl.edu.icm.unity.types.EntityInformation;
import pl.edu.icm.unity.types.EntityScheduledOperation;
import pl.edu.icm.unity.types.EntityState;

public class EntityTest extends AbstractBasicDAOTest<EntityInformation>
{
	@Autowired
	private EntityDAO dao;
	
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
	protected void mutateObject(EntityInformation src)
	{
		src.setEntityState(EntityState.authenticationDisabled);
		src.setRemovalByUserTime(new Date(3000));
		src.setScheduledOperation(EntityScheduledOperation.REMOVE);
		src.setScheduledOperationTime(new Date(5000));
	}

	@Override
	protected void assertAreEqual(EntityInformation obj, EntityInformation cmp)
	{
		assertThat(obj, is(cmp));
	}
	
	@Test
	public void insertedWithIdIsReturned()
	{
		tx.runInTransaction(() -> {
			EntityInformation obj = getObject("name1");
			long key = obj.getId();
			dao.createWithId(obj);

			EntityInformation ret = dao.getByKey(key);

			assertThat(obj.getId(), is(key));
			assertThat(ret, is(notNullValue()));
			assertAreEqual(obj, ret);
		});
	}

	@Test
	public void regularInsertAfterInsertedWithIdSucceeds()
	{
		tx.runInTransaction(() -> {
			EntityInformation obj = getObject("name1");
			long key = obj.getId();
			dao.createWithId(obj);

			EntityInformation obj2 = getObject("");
			dao.create(obj2);
			
			assertThat(obj2.getId() != key, is(true));
		});
	}
}
