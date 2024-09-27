/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.json.dump.DBDumpContentElements;
import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.api.BasicCRUDDAO;
import pl.edu.icm.unity.store.api.ImportExport;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.tx.TransactionTL;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
public abstract class AbstractBasicDAOTest<T>
{
	@Autowired
	protected StorageCleanerImpl dbCleaner;

	@Autowired
	protected TransactionalRunner tx;
	
	@Autowired
	protected ImportExport ie;
	
	@BeforeEach
	public void cleanDB()
	{
		dbCleaner.cleanOrDelete();
	}

	
	@AfterEach
	public void shutdown()
	{
		dbCleaner.shutdown();
	}
	
	protected abstract BasicCRUDDAO<T> getDAO();
	protected abstract T getObject(String id);
	protected abstract T mutateObject(T src);

	@Test
	public void shouldReturnCreatedByKey()
	{
		tx.runInTransaction(() -> {
			BasicCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");

			long key1 = dao.create(obj);
			dao.deleteByKey(key1);
			long key2 = dao.create(obj);
			T ret = dao.getByKey(key2);

			assertThat(key1 != key2).isTrue();
			assertThat(ret).isNotNull();
			assertThat(ret).isEqualTo(obj);
		});
	}
	
	@Test
	public void shouldReturnUpdatedByKey()
	{
		tx.runInTransaction(() -> {
			BasicCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");
			long key = dao.create(obj);

			T updated = mutateObject(obj);
			dao.updateByKey(key, updated);

			T ret = dao.getByKey(key);

			assertThat(ret).isNotNull();
			assertThat(ret).isEqualTo(updated);
		});
	}

	@Test
	public void shouldReturnTwoCreatedWithinCollections()
	{
		tx.runInTransaction(() -> {
			BasicCRUDDAO<T> dao = getDAO();
			int initial = dao.getAll().size();
			T obj = getObject("name1");
			T obj2 = getObject("name2");

			dao.create(obj);
			dao.create(obj2);
			List<T> all = dao.getAll();

			assertThat(all).isNotNull();

			assertThat(all.size()).isEqualTo(initial + 2);

			boolean objFound = false;
			boolean obj2Found = false;
			for (T el: all)
			{
				if (el.equals(obj))
					objFound = true;
				if (el.equals(obj2))
					obj2Found = true;
			}
			assertThat(objFound).isTrue();
			assertThat(obj2Found).isTrue();
		});
	}

	@Test
	public void shouldReturnTwoCreatedWithinCollectionsWithCommit()
	{
		tx.runInTransaction(() -> {
			BasicCRUDDAO<T> dao = getDAO();
			int initial = dao.getAll().size();
			T obj = getObject("name1");
			T obj2 = getObject("name2");

			dao.create(obj);
			dao.create(obj2);
	
			TransactionTL.manualCommit();
			
			List<T> all = dao.getAll();

			assertThat(all).isNotNull();

			assertThat(all.size()).isEqualTo(initial + 2);

			boolean objFound = false;
			boolean obj2Found = false;
			for (T el: all)
			{
				if (el.equals(obj))
					objFound = true;
				if (el.equals(obj2))
					obj2Found = true;
			}
			assertThat(objFound).isTrue();
			assertThat(obj2Found).isTrue();
		});
	}
	
	@Test
	public void shouldNotReturnRemovedByKey()
	{
		tx.runInTransaction(() -> {
			BasicCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");
			long key = dao.create(obj);

			dao.deleteByKey(key);

			Throwable error = catchThrowable(() -> dao.getByKey(key));

			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}
	
	@Test
	public void shouldNotReturnBulkRemoved()
	{
		tx.runInTransaction(() -> {
			BasicCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");
			long key = dao.create(obj);

			dao.deleteAll();

			Throwable error = catchThrowable(() -> dao.getByKey(key));
			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}
	
	@Test
	public void shouldFailOnRemovingAbsent()
	{
		tx.runInTransaction(() -> {
			BasicCRUDDAO<T> dao = getDAO();

			Throwable error = catchThrowable(() -> dao.deleteByKey(Integer.MAX_VALUE));

			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}

	@Test
	public void shouldFailOnUpdatingAbsent()
	{
		tx.runInTransaction(() -> {
			BasicCRUDDAO<T> dao = getDAO();
			T obj = getObject("name1");

			Throwable error = catchThrowable(() -> dao.updateByKey(Integer.MAX_VALUE, obj));

			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}
	
	@Test
	public void importExportIsIdempotent()
	{
		importExportIsIdempotent(id -> {});
	}
	
	protected void insertedListIsReturned()
	{
		tx.runInTransaction(() -> {
			T obj1 = getObject("name1");
			T obj2 = getObject("name2");
			BasicCRUDDAO<T> dao = getDAO();
			dao.createList(Lists.newArrayList(obj1, obj2));

			List<T> ret = dao.getAll();

			assertThat(ret).isNotNull();
			assertThat(ret.size()).isEqualTo(2);
			assertThat(ret).contains(obj1, obj2);
		});
	}
	
	protected T importExportIsIdempotent(Consumer<Long> preExportEntityIdConsumer)
	{
		// given
		T obj = getObject("name1");
		ByteArrayOutputStream os = export(obj,  preExportEntityIdConsumer);
		cleanDb();
		
		// when
		load(os);
		
		// then
		assertImportWasSuccessful(obj);
		
		return obj;
	}
	
	private void cleanDb()
	{
		tx.runInTransaction(() -> {
			dbCleaner.cleanOrDelete();
		});
	}

	private void assertImportWasSuccessful(T obj)
	{
		tx.runInTransaction(() -> {
			BasicCRUDDAO<T> dao = getDAO();
			List<T> all = dao.getAll();
			assertThat(all.size()).isEqualTo(1);
			assertThat(all.get(0)).isEqualTo(obj);
		});
	}
	
	private void load(ByteArrayOutputStream os)
	{
		tx.runInTransaction(() -> {
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
		});
	}
	
	private ByteArrayOutputStream export(T obj, Consumer<Long> beforeExportEntityIdConsumer)
	{
		return tx.runInTransactionRet(() -> {
			BasicCRUDDAO<T> dao = getDAO();
			long id = dao.create(obj);
			beforeExportEntityIdConsumer.accept(id);
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try
			{
				ie.store(baos, new DBDumpContentElements());
			} catch (Exception e)
			{
				e.printStackTrace();
				fail("Export failed " + e);
			}
			return baos;
		});
	}
}
