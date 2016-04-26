/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.base.internal.TransactionalRunner;
import pl.edu.icm.unity.store.api.BasicCRUDDAO;
import pl.edu.icm.unity.store.api.NamedCRUDDAO;
import pl.edu.icm.unity.types.NamedObject;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
public abstract class AbstractNamedDAOTest<T extends NamedObject>
{
	@Autowired
	private StorageCleaner dbCleaner;

	@Autowired
	protected TransactionalRunner tx;

	@Before
	public void cleanDB()
	{
		dbCleaner.reset();
	}

	protected abstract NamedCRUDDAO<T> getDAO();
	protected abstract T getObject(String name);
	protected abstract void mutateObject(T src);
	protected abstract String getName(T obj);
	protected abstract void assertAreEqual(T obj, T cmp);

	@Test
	public void shouldReturnCreatedByName()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");

			dao.create(obj);
			T ret = dao.get(getName(obj));

			assertThat(ret, is(notNullValue()));
			assertAreEqual(obj, ret);
		});
	}
	
	@Test
	public void createdExists()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");

			dao.create(obj);
			boolean ret = dao.exists(getName(obj));

			assertThat(ret, is(true));
		});
	}
	
	@Test
	public void shouldReturnCreatedByKey()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");

			long key1 = dao.create(obj);
			dao.deleteByKey(key1);
			long key2 = dao.create(obj);
			T ret = dao.getByKey(key2);

			assertThat(key1 != key2, is(true));
			assertThat(ret, is(notNullValue()));
			assertAreEqual(obj, ret);
		});
	}
	
	@Test
	public void shouldReturnUpdatedByName()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");
			dao.create(obj);

			mutateObject(obj);
			dao.update(obj);

			T ret = dao.get(getName(obj));

			assertThat(ret, is(notNullValue()));
			assertAreEqual(obj, ret);
		});
	}

	@Test
	public void shouldReturnUpdatedByKey()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");
			long key = dao.create(obj);

			mutateObject(obj);
			dao.updateByKey(key, obj);

			T ret = dao.getByKey(key);

			assertThat(ret, is(notNullValue()));
			assertAreEqual(obj, ret);
		});
	}

	@Test
	public void shouldReturnTwoCreatedWithinCollections()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");
			T obj2 = getObject("name2");

			dao.create(obj);
			dao.create(obj2);
			List<T> all = dao.getAll();
			Map<String, T> asMap = dao.getAsMap();

			assertThat(all, is(notNullValue()));
			assertThat(asMap, is(notNullValue()));

			assertThat(all.size(), is(2));
			assertThat(asMap.size(), is(2));

			assertThat(asMap.containsKey("name1"), is(true));
			assertThat(asMap.containsKey("name2"), is(true));

			assertAreEqual(asMap.get("name1"), obj);
			assertAreEqual(asMap.get("name2"), obj2);

			T fromList = all.get(0);
			T fromList2 = all.get(1);
			if (!getName(fromList).equals("name1"))
			{
				fromList = all.get(1);
				fromList2 = all.get(0);
			}

			assertAreEqual(fromList, obj);
			assertAreEqual(fromList2, obj2);
		});
	}

	@Test
	public void shouldNotReturnRemovedByName()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");
			dao.create(obj);

			dao.delete(getName(obj));

			catchException(dao).get(getName(obj));

			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}
	
	@Test
	public void shouldNotReturnRemovedByKey()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");
			long key = dao.create(obj);

			dao.deleteByKey(key);

			catchException(dao).getByKey(key);

			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}
	@Test
	public void shouldFailOnRemovingAbsent()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");

			catchException(dao).delete(getName(obj));

			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}

	@Test
	public void shouldFailOnCreatingPresent()
	{
		tx.runInTransaction(() -> {
			BasicCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");
			dao.create(obj);
			catchException(dao).create(obj);
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}

	@Test
	public void shouldFailOnUpdatingAbsent()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");

			catchException(dao).update(obj);

			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}
}
