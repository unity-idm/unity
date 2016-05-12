/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.internal.TransactionalRunner;
import pl.edu.icm.unity.store.StorageCleaner;
import pl.edu.icm.unity.store.api.generic.GenericObjectsDAO;
import pl.edu.icm.unity.types.NamedObject;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
public abstract class AbstractObjStoreTest<T extends NamedObject>
{
	@Autowired
	protected StorageCleaner dbCleaner;

	@Autowired
	protected TransactionalRunner tx;
	
	@Before
	public void cleanDB()
	{
		dbCleaner.reset();
	}

	protected abstract GenericObjectsDAO<T> getDAO();
	protected abstract T getObject(String id);
	protected abstract void mutateObject(T src);
	protected abstract void assertAreEqual(T obj, T cmp);

	@Test
	public void createdExists()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();
			T obj = getObject("name1");

			dao.insert(obj);
			boolean ret = dao.exists("name1");

			assertThat(ret, is(true));
		});
	}

	@Test
	public void createdExistsInAssert()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();
			T obj = getObject("name1");

			dao.insert(obj);
			dao.assertExist(Lists.newArrayList("name1"));
		});
	}

	@Test
	public void missingBreaksAssert()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();

			catchException(dao).assertExist(Lists.newArrayList("name1"));
			
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}

	@Test
	public void notCreatedDoesntExist()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();

			boolean ret = dao.exists("name1");

			assertThat(ret, is(false));
		});
	}

	@Test
	public void shouldReturnCreatedByName()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();
			T obj = getObject("name1");

			dao.insert(obj);
			T ret = dao.get("name1");

			assertThat(ret, is(notNullValue()));
			assertAreEqual(obj, ret);
		});
	}

	@Test
	public void shouldReturnCreatedWithTimestamp()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();
			T obj = getObject("name1");

			dao.insert(obj);
			List<Entry<T, Date>> ret = dao.getAllWithUpdateTimestamps();

			assertThat(ret, is(notNullValue()));
			assertThat(ret.size(), is(1));
			assertAreEqual(obj, ret.get(0).getKey());
			assertThat(ret.get(0).getValue(), is(notNullValue()));
		});
	}
	
	@Test
	public void shouldReturnAllCreatedByName()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();
			T obj = getObject("name1");

			dao.insert(obj);
			List<T> ret = dao.getAll();

			assertThat(ret, is(notNullValue()));
			assertThat(ret.size(), is(1));
			assertAreEqual(ret.get(0), obj);
		});
	}
	
	@Test
	public void shouldReturnUpdated()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();
			T obj = getObject("name1");
			dao.insert(obj);

			mutateObject(obj);
			dao.update("name1", obj);

			T ret = dao.get(obj.getName());

			assertThat(ret, is(notNullValue()));
			assertAreEqual(obj, ret);
		});
	}
	
	@Test
	public void shouldReturnUpdatedTS()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();
			T obj = getObject("name1");
			dao.insert(obj);

			mutateObject(obj);
			dao.updateTS("name1");

			List<Entry<String, Date>> ret = dao.getAllNamesWithUpdateTimestamps();

			assertThat(ret, is(notNullValue()));
			assertThat(ret.size(), is(1));
			assertThat(ret.get(0).getKey(), is("name1"));
			assertThat(System.currentTimeMillis() - ret.get(0).getValue().getTime() < 5000, is(true));
		});
	}
	
	@Test
	public void shouldReturnTwoCreatedWithinList()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();
			int initial = dao.getAll().size();
			T obj = getObject("name1");
			T obj2 = getObject("name2");

			dao.insert(obj);
			dao.insert(obj2);
			
			List<T> all = dao.getAll();

			assertThat(all, is(notNullValue()));

			assertThat(all.size(), is(initial + 2));

			boolean objFound = false;
			boolean obj2Found = false;
			for (T el: all)
			{
				try
				{
					assertAreEqual(el, obj);
					objFound = true;
				} catch (Throwable t) {}
				try
				{
					assertAreEqual(el, obj2);
					obj2Found = true;
				} catch (Throwable t) {}
			}
			assertThat(objFound, is(true));
			assertThat(obj2Found, is(true));
		});
	}

	@Test
	public void shouldReturnTwoCreatedWithinMap()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();
			int initial = dao.getAll().size();
			T obj = getObject("name1");
			T obj2 = getObject("name2");

			dao.insert(obj);
			dao.insert(obj2);
			
			Map<String, T> all = dao.getAllAsMap();
			Set<String> allNames = dao.getAllNames();

			assertThat(all, is(notNullValue()));

			assertThat(all.size(), is(initial + 2));
			assertThat(all.get("name1"), is(obj));
			assertThat(all.get("name2"), is(obj2));
			assertThat(allNames, is(all.keySet()));
		});
	}

	@Test
	public void shouldNotReturnRemoved()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();
			T obj = getObject("name1");
			dao.insert(obj);

			dao.remove("name1");

			catchException(dao).get("name1");
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}

	@Test
	public void shouldNotReturnBulkRemoved()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();
			T obj = getObject("name1");
			dao.insert(obj);

			dao.removeAllNoCheck();

			catchException(dao).get("name1");
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}
	
	@Test
	public void shouldFailOnRemovingAbsent()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();

			catchException(dao).remove("name1");

			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}

	@Test
	public void shouldFailOnUpdatingAbsent()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();
			T obj = getObject("name1");

			catchException(dao).update("missing", obj);

			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}

	@Test
	public void shouldFailOnUpdatingAbsentTS()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();

			catchException(dao).updateTS("missing");

			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}

}
