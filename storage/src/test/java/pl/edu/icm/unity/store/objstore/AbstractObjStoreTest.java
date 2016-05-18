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
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
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
import pl.edu.icm.unity.store.api.ImportExport;
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
	
	@Autowired
	protected ImportExport ie;
	
	@Before
	public void cleanDB()
	{
		dbCleaner.reset();
	}

	protected abstract GenericObjectsDAO<T> getDAO();
	protected abstract T getObject(String id);
	protected abstract T mutateObject(T src);
	protected abstract void assertAreEqual(T obj, T cmp);

	@Test
	public void createdExists()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();
			T obj = getObject("name1");

			dao.create(obj);
			boolean ret = dao.exists(obj.getName());

			assertThat(ret, is(true));
		});
	}

	@Test
	public void createdExistsInAssert()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();
			T obj = getObject("name1");

			dao.create(obj);
			dao.assertExist(Lists.newArrayList(obj.getName()));
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

			dao.create(obj);
			T ret = dao.get(obj.getName());

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

			dao.create(obj);
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

			dao.create(obj);
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
			dao.create(obj);
			String originalName = obj.getName();
			
			T changed = mutateObject(obj);
			dao.update(originalName, changed);

			T ret = dao.get(changed.getName());

			assertThat(ret, is(notNullValue()));
			assertAreEqual(changed, ret);
		});
	}
	
	@Test
	public void shouldReturnUpdatedTS()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();
			T obj = getObject("name1");
			dao.create(obj);

			dao.updateTS(obj.getName());

			List<Entry<String, Date>> ret = dao.getAllNamesWithUpdateTimestamps();

			assertThat(ret, is(notNullValue()));
			assertThat(ret.size(), is(1));
			assertThat(ret.get(0).getKey(), is(obj.getName()));
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

			dao.create(obj);
			dao.create(obj2);
			
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

			dao.create(obj);
			dao.create(obj2);
			
			Map<String, T> all = dao.getAllAsMap();
			Set<String> allNames = dao.getAllNames();

			assertThat(all, is(notNullValue()));

			assertThat(all.size(), is(initial + 2));
			assertThat(all.get(obj.getName()), is(obj));
			assertThat(all.get(obj2.getName()), is(obj2));
			assertThat(allNames, is(all.keySet()));
		});
	}

	@Test
	public void shouldNotReturnRemoved()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();
			T obj = getObject("name1");
			dao.create(obj);

			dao.delete(obj.getName());

			catchException(dao).get(obj.getName());
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}

	@Test
	public void shouldNotReturnBulkRemoved()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();
			T obj = getObject("name1");
			dao.create(obj);

			dao.deleteAllNoCheck();

			catchException(dao).get(obj.getName());
			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}
	
	@Test
	public void shouldFailOnRemovingAbsent()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();

			catchException(dao).delete("name1");

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
	
	@Test
	public void importExportIsIdempotent()
	{
		tx.runInTransaction(() -> {
			GenericObjectsDAO<T> dao = getDAO();
			T obj = getObject("name1");
			dao.create(obj);
			
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			try
			{
				ie.store(os);
			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Export failed " + e);
			}

			dbCleaner.reset();
			
			String dump = new String(os.toByteArray(), StandardCharsets.UTF_8);
			ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
			try
			{
				ie.load(is);
			} catch (Exception e)
			{
				e.printStackTrace();
				
				fail("Import failed " + e + "\nDump:\n" + dump);
			}

			List<T> all = dao.getAll();

			assertThat(all.size(), is(1));
			assertAreEqual(all.get(0), obj);
		});
	}
}
