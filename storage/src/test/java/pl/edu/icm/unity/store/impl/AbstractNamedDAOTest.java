/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.store.api.BasicCRUDDAO;
import pl.edu.icm.unity.store.api.NamedCRUDDAO;
import pl.edu.icm.unity.store.tx.TransactionTL;
import pl.edu.icm.unity.types.NamedObject;

public abstract class AbstractNamedDAOTest<T extends NamedObject> extends AbstractBasicDAOTest<T>
{
	@Override
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
			assertThat(ret, is(obj));
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
	public void notCreatedDoesntExist()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();

			boolean ret = dao.exists("name1");

			assertThat(ret, is(false));
		});
	}


	@Test
	public void createdExistsInAssert()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");

			dao.create(obj);
			dao.assertExist(Lists.newArrayList(obj.getName()));
		});
	}

	@Test
	public void missingBreaksAssert()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();

			Throwable error = catchThrowable(() -> dao.assertExist(Lists.newArrayList("name1")));
			
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}

	@Test
	public void shouldReturnUpdated()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");
			dao.create(obj);
			String originalName = obj.getName();
			
			T changed = mutateObject(obj);
			dao.updateByName(originalName, changed);

			TransactionTL.manualCommit();
			
			T ret = dao.get(changed.getName());

			assertThat(ret, is(notNullValue()));
			assertThat(ret, is(changed));
			assertThat(changed != ret, is(true));
		});
	}
	
	@Test
	public void shouldReturnTwoCreatedWithinCollectionsByName()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();
			int initial = dao.getAll().size();
			T obj = getObject("name1");
			T obj2 = getObject("name2");

			dao.create(obj);
			dao.create(obj2);
			Map<String, T> asMap = dao.getAllAsMap();

			assertThat(asMap, is(notNullValue()));

			assertThat(asMap.size(), is(2 + initial));

			assertThat(asMap.containsKey(obj.getName()), is(true));
			assertThat(asMap.containsKey(obj2.getName()), is(true));

			assertThat(asMap.get(obj.getName()), is(obj));
			assertThat(asMap.get(obj2.getName()), is(obj2));
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

			Throwable error = catchThrowable(() -> dao.get(obj.getName()));

			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}
	
	@Test
	public void shouldFailOnRemovingAbsentByName()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");

			Throwable error = catchThrowable(() -> dao.delete(obj.getName()));

			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}

	@Test
	public void shouldFailOnCreatingPresent()
	{
		tx.runInTransaction(() -> {
			BasicCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");
			dao.create(obj);
			Throwable error = catchThrowable(() -> dao.create(obj));
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}

	@Test
	public void shouldFailOnUpdatingAbsentByName()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");

			Throwable error = catchThrowable(() -> dao.update(obj));

			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}

	@Test
	public void shouldFailOnCreatingWithTooLongName()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();
			T obj = getObject(genTooLongName());

			Throwable error = catchThrowable(() -> dao.create(obj));
			
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}

	@Test
	public void shouldFailOnUpdatingToTooLongName()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");
			long key = dao.create(obj);
			
			T obj2 = getObject(genTooLongName());
			Throwable error = catchThrowable(() -> dao.updateByKey(key, obj2));

			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}
	
	private String genTooLongName()
	{
		char[] name = new char[StorageLimits.NAME_LIMIT+1];
		for (int i=0; i<name.length; i++)
			name[i] = 'a';
		return new String(name);
	}
}
