/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Test;

import pl.edu.icm.unity.base.describedObject.NamedObject;
import pl.edu.icm.unity.base.json.dump.DBDumpContentElements;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.impl.AbstractNamedDAOTest;

public abstract class AbstractNamedWithTSTest<T extends NamedObject> extends AbstractNamedDAOTest<T>
{
	@Override
	protected abstract NamedCRUDDAOWithTS<T> getDAO();

	@Test
	public void shouldReturnCreatedWithTimestamp()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAOWithTS<T> dao = getDAO();
			T obj = getObject("name1");

			dao.create(obj);
			List<Entry<T, Date>> ret = dao.getAllWithUpdateTimestamps();

			assertThat(ret, is(notNullValue()));
			assertThat(ret.size(), is(1));
			assertThat(ret.get(0).getKey(), is (obj));
			assertThat(ret.get(0).getValue(), is(notNullValue()));
		});
	}
	
	
	@Test
	public void shouldReturnUpdatedTS()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAOWithTS<T> dao = getDAO();
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
	public void shouldFailOnUpdatingAbsentTS()
	{
		tx.runInTransaction(() -> {
			NamedCRUDDAOWithTS<T> dao = getDAO();

			Throwable error = catchThrowable(() -> dao.updateTS("missing"));

			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}
	
	/**
	 * Besides the standard test, checks also if the update timestamp is preserved
	 */
	@Override
	@Test
	public void importExportIsIdempotent()
	{
		T obj = getObject("name1");
		ByteArrayOutputStream os = tx.runInTransactionRet(() -> {
			NamedCRUDDAOWithTS<T> dao = getDAO();
			dao.create(obj);
	
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
		Date updateTimestamp = tx.runInTransactionRet(() -> {
			NamedCRUDDAOWithTS<T> dao = getDAO();
			return dao.getUpdateTimestamp(obj.getName());
		});

		tx.runInTransaction(() -> {
			dbCleaner.cleanOrDelete();
		});			

		tx.runInTransaction(() -> {
			NamedCRUDDAOWithTS<T> dao = getDAO();
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
			assertThat(all.get(0), is(obj));
			
			Date updateTimestamp2 = dao.getUpdateTimestamp(obj.getName());
			assertThat(updateTimestamp2, is(updateTimestamp));
		});
	}
}
