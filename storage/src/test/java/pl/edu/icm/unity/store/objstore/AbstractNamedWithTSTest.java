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
import java.util.Map.Entry;

import org.junit.Test;

import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.impl.AbstractNamedDAOTest;
import pl.edu.icm.unity.types.NamedObject;

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

			catchException(dao).updateTS("missing");

			assertThat(caughtException(), isA(IllegalArgumentException.class));
		});
	}
}
