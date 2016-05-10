/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.objstore;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.store.AbstractBasicDAOTest;

public class ObjectStoreTest extends AbstractBasicDAOTest<GenericObjectBean>
{
	@Autowired
	private ObjectStoreDAO dao;
	
	@Override
	protected ObjectStoreDAO getDAO()
	{
		return dao;
	}
	
	
	@Test
	public void allNamesOfTypeAreReturned()
	{
		tx.runInTransaction(() -> {
			GenericObjectBean obj = getObject("name1");
			dao.create(obj);
			GenericObjectBean obj2 = getObject("name2");
			obj2.setType("type2");
			dao.create(obj2);
			
			Set<String> ret = dao.getNamesOfType("type");

			assertThat(ret, is(notNullValue()));
			assertThat(ret.size(), is(1));
			assertThat(ret.iterator().next(), is("name1"));
		});
	}
	
	@Test
	public void allObjectsOfTypeAreReturned()
	{
		tx.runInTransaction(() -> {
			GenericObjectBean obj = getObject("name1");
			dao.create(obj);
			GenericObjectBean obj2 = getObject("name2");
			obj2.setType("type2");
			dao.create(obj2);

			List<GenericObjectBean> ret = dao.getObjectsOfType("type");

			assertThat(ret, is(notNullValue()));
			assertThat(ret.size(), is(1));
			assertThat(ret.get(0), is(obj));
		});
	}

	@Test
	public void objectIsReturnedByNameType()
	{
		tx.runInTransaction(() -> {
			GenericObjectBean obj = getObject("name1");
			dao.create(obj);

			GenericObjectBean ret = dao.getObjectByNameType("name1", "type");

			assertThat(ret, is(notNullValue()));
			assertThat(ret, is(obj));
		});
	}

	@Test
	public void missingObjectRetrievalFails()
	{
		tx.runInTransaction(() -> {
			catchException(dao).getObjectByNameType("name1", "type");

			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}

	@Test
	public void allTypesAreReturned()
	{
		tx.runInTransaction(() -> {
			GenericObjectBean obj = getObject("name1");
			dao.create(obj);
			GenericObjectBean obj2 = getObject("name2");
			obj2.setType("type2");
			dao.create(obj2);
			
			Set<String> ret = dao.getObjectTypes();

			assertThat(ret, is(notNullValue()));
			assertThat(ret.size(), is(2));
			assertThat(ret.contains("type"), is(true));
			assertThat(ret.contains("type2"), is(true));
		});
	}

	@Test
	public void objectRemovedByTypeIsNotReturned()
	{
		tx.runInTransaction(() -> {
			GenericObjectBean obj = getObject("name1");
			long key = dao.create(obj);
			GenericObjectBean obj2 = getObject("name2");
			obj2.setType("type2");
			dao.create(obj2);
			
			dao.removeObjectsByType("type");

			catchException(dao).getByKey(key);
			assertThat(caughtException(), isA(IllegalArgumentException.class));
			assertThat(dao.getAll().size(), is(1));
		});
	}
	
	@Test
	public void objectRemovedByNameTypeIsNotReturned()
	{
		tx.runInTransaction(() -> {
			GenericObjectBean obj = getObject("name1");
			long key = dao.create(obj);
			GenericObjectBean obj2 = getObject("name2");
			obj2.setType("type2");
			dao.create(obj2);
			
			dao.removeObject("name1", "type");

			catchException(dao).getByKey(key);
			assertThat(caughtException(), isA(IllegalArgumentException.class));
			assertThat(dao.getAll().size(), is(1));
		});
	}

	@Test
	public void removalOfMissingObjectFails()
	{
		tx.runInTransaction(() -> {
			catchException(dao).removeObject("name1", "type");

			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}

	@Test
	public void objectUpdatedByNameTypeIsReturned()
	{
		tx.runInTransaction(() -> {
			GenericObjectBean obj = getObject("name1");
			long key = dao.create(obj);
			
			dao.updateObject("name1", "type", new byte[] {'z'});

			GenericObjectBean ret = dao.getByKey(key);
			assertThat(dao.getAll().size(), is(1));
			assertThat(ret.getContents(), is(new byte[] {'z'}));
			assertThat(System.currentTimeMillis() - ret.getLastUpdate().getTime() < 5000, is(true));
		});
	}

	@Test
	public void updateOfMissingObjectFails()
	{
		tx.runInTransaction(() -> {
			catchException(dao).updateObject("name1", "type", new byte[] {});

			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});	
	}
	
	@Override
	protected GenericObjectBean getObject(String id)
	{
		GenericObjectBean ret = new GenericObjectBean(id, new byte[] {'a'}, "type", "subType");
		ret.setLastUpdate(new Date(100));
		return ret;
	}

	@Override
	protected void mutateObject(GenericObjectBean src)
	{
		src.setContents(new byte[] {'b', 'c'});
		src.setLastUpdate(new Date(1000));
	}

	@Override
	protected void assertAreEqual(GenericObjectBean obj, GenericObjectBean cmp)
	{
		assertThat(obj, is(cmp));
	}
}
