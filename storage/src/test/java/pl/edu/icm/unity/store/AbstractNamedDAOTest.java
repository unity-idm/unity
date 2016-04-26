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

import java.util.Map;

import org.junit.Test;

import pl.edu.icm.unity.store.api.BasicCRUDDAO;
import pl.edu.icm.unity.store.api.NamedCRUDDAO;
import pl.edu.icm.unity.types.NamedObject;

public abstract class AbstractNamedDAOTest<T extends NamedObject> extends AbstractBasicDAOTest<T>
{
	protected abstract NamedCRUDDAO<T> getDAO();
	
	@Test
	public void shouldReturnCreatedByName()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");

			dao.create(obj);
			T ret = dao.get(obj.getName());

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
			boolean ret = dao.exists(obj.getName());

			assertThat(ret, is(true));
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

			T ret = dao.get(obj.getName());

			assertThat(ret, is(notNullValue()));
			assertAreEqual(obj, ret);
		});
	}

	@Test
	public void shouldReturnTwoCreatedWithinCollectionsByName()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");
			T obj2 = getObject("name2");

			dao.create(obj);
			dao.create(obj2);
			Map<String, T> asMap = dao.getAsMap();

			assertThat(asMap, is(notNullValue()));

			assertThat(asMap.size(), is(2));

			assertThat(asMap.containsKey("name1"), is(true));
			assertThat(asMap.containsKey("name2"), is(true));

			assertAreEqual(asMap.get("name1"), obj);
			assertAreEqual(asMap.get("name2"), obj2);
		});
	}

	@Test
	public void shouldNotReturnRemovedByName()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");
			dao.create(obj);

			dao.delete(obj.getName());

			catchException(dao).get(obj.getName());

			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}
	
	@Test
	public void shouldFailOnRemovingAbsentByName()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");

			catchException(dao).delete(obj.getName());

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
	public void shouldFailOnUpdatingAbsentByName()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");

			catchException(dao).update(obj);

			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}
}
