/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.objstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.store.impl.AbstractBasicDAOTest;

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
	public void missingObjectRetrievalReturnNull()
	{
		tx.runInTransaction(() -> {
			GenericObjectBean ret = dao.getObjectByNameType("name1", "type");

			assertThat(ret, is(nullValue()));
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

			Throwable error = catchThrowable(() -> dao.getByKey(key));
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
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

			Throwable error = catchThrowable(() -> dao.getByKey(key));
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
			assertThat(dao.getAll().size(), is(1));
		});
	}

	@Test
	public void removalOfMissingObjectFails()
	{
		tx.runInTransaction(() -> {
			Throwable error = catchThrowable(() -> dao.removeObject("name1", "type"));

			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}

	@Test
	public void objectUpdatedByNameTypeIsReturned()
	{
		tx.runInTransaction(() -> {
			GenericObjectBean obj = getObject("name1");
			long key = dao.create(obj);
			
			obj.setContents(new byte[] {'z'});
			obj.setLastUpdate(new Date());
			dao.updateObject("name1", "type", obj);

			GenericObjectBean ret = dao.getByKey(key);
			assertThat(dao.getAll().size(), is(1));
			assertThat(ret.getContents(), is(new byte[] {'z'}));
			assertThat(ret.getLastUpdate(), is(obj.getLastUpdate()));
		});
	}

	@Test
	public void updateOfMissingObjectFails()
	{
		tx.runInTransaction(() -> {
			Throwable error = catchThrowable(() -> dao.updateObject("name1", "type", getObject("name1")));

			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});	
	}
	
	@Test
	@Override
	public void importExportIsIdempotent()
	{
		//generic objects are exported and imported in a different way, this is tested 
		//within object store abstraction
	}
	
	@Override
	protected GenericObjectBean getObject(String id)
	{
		GenericObjectBean ret = new GenericObjectBean(id, new byte[] {'a'}, "type");
		ret.setLastUpdate(new Date(100));
		return ret;
	}

	@Override
	protected GenericObjectBean mutateObject(GenericObjectBean src)
	{
		src.setContents(new byte[] {'b', 'c'});
		src.setLastUpdate(new Date(2000));
		return src;
	}
}
