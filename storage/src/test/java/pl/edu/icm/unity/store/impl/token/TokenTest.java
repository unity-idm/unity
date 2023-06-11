/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.identity.EntityInformation;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.TokenDAO;
import pl.edu.icm.unity.store.impl.AbstractBasicDAOTest;

public class TokenTest extends AbstractBasicDAOTest<Token>
{
	@Autowired
	private TokenDAO dao;
	@Autowired
	private EntityDAO entityDAO;
	
	private long entityId;
	private long entityId2;

	@Before
	public void cleanDB()
	{
		dbCleaner.cleanOrDelete();
		tx.runInTransaction(() -> {
			entityId = entityDAO.create(new EntityInformation());
			entityId2 = entityDAO.create(new EntityInformation());
		});
	}

	
	@Test
	public void tokenRemovedByIdIsNotReturned()
	{
		tx.runInTransaction(() -> {
			Token obj = getObject("name1");
			long key = dao.create(obj);

			dao.delete(obj.getType(), obj.getValue());

			Throwable error = catchThrowable(() -> dao.getByKey(key));

			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}
	
	@Test
	public void removalOfMissingTokenByValueFails()
	{
		tx.runInTransaction(() -> {
			Token obj = getObject("name1");
			dao.create(obj);

			Throwable error = catchThrowable(() -> dao.delete(obj.getType(), obj.getValue() + "__OTHER"));

			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}

	@Test
	public void removalOfMissingTokenByTypeFails()
	{
		tx.runInTransaction(() -> {
			Token obj = getObject("name1");
			dao.create(obj);

			Throwable error = catchThrowable(() -> dao.delete(obj.getType() + "__OTHER", obj.getValue()));

			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}
	
	@Test
	public void tokenUpdatedByIdIsReturned()
	{
		tx.runInTransaction(() -> {
			Token obj = getObject("name1");
			long key = dao.create(obj);

			mutateObject(obj);
			dao.update(obj);

			Token ret = dao.getByKey(key);

			assertThat(ret, is(notNullValue()));
			assertThat(ret, is(obj));
		});
	}

	@Test
	public void tokenSelectedByIdIsReturned()
	{
		tx.runInTransaction(() -> {
			Token obj = getObject("name1");
			dao.create(obj);

			Token ret = dao.get(obj.getType(), obj.getValue());

			assertThat(ret, is(notNullValue()));
			assertThat(ret, is(obj));
		});
	}	
	
	@Test
	public void missingTokenSelectedByValueFails()
	{
		tx.runInTransaction(() -> {
			Token obj = getObject("name1");
			dao.create(obj);

			Throwable error = catchThrowable(() -> dao.get(obj.getType(), obj.getValue() + "__CHANGED"));

			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}
	
	@Test
	public void missingTokenSelectedByTypeFails()
	{
		tx.runInTransaction(() -> {
			Token obj = getObject("name1");
			dao.create(obj);

			Throwable error = catchThrowable(() -> dao.get(obj.getType() + "__CHANGED", obj.getValue()));

			assertThat(error).isInstanceOf(IllegalArgumentException.class);
		});
	}
	
	@Test
	public void onlyTokensOfGivenTypeAreReturned()
	{
		tx.runInTransaction(() -> {
			Token obj = getObject("type1", "name", entityId);
			dao.create(obj);
			Token obj2 = getObject("type2", "name", entityId);
			dao.create(obj2);

			List<Token> byType = dao.getByType("type1");

			assertThat(byType.size(), is(1));
			assertThat(byType.get(0), is(obj));
		});
	}	

	@Test
	public void onlyOwnedTokensOfGivenTypeAreReturned()
	{
		tx.runInTransaction(() -> {
			Token obj = getObject("type1", "name", entityId);
			dao.create(obj);
			Token obj2 = getObject("type1", "name2", entityId2);
			dao.create(obj2);

			List<Token> byType = dao.getOwned("type1", entityId);

			assertThat(byType.size(), is(1));
			assertThat(byType.get(0), is(obj));
		});
	}	

	@Test
	public void onlyExpiredTokensAreReturned()
	{
		tx.runInTransaction(() -> {
			Token expired = getObject("type1", "name", entityId);
			dao.create(expired);
			Token obj2 = getObject("type1", "name2", entityId2);
			obj2.setExpires(new Date(System.currentTimeMillis() + 100000));
			dao.create(obj2);

			List<Token> byType = dao.getExpired();

			assertThat(byType.size(), is(1));
			assertThat(byType.get(0), is(expired));
		});
	}	

	@Override
	@Test
	public void importExportIsIdempotent()
	{
		//empty - tokens are not subject of import/export
	}
	
	@Override
	protected TokenDAO getDAO()
	{
		return dao;
	}

	@Override
	protected Token getObject(String id)
	{
		return getObject("type", id, entityId);
	}

	protected Token getObject(String type, String id, long owner)
	{
		Token ret = new Token(type, id, owner);
		ret.setContents(new byte[] {'a'});
		ret.setCreated(new Date(100));
		ret.setExpires(new Date(1000));
		return ret;
	}

	@Override
	protected Token mutateObject(Token src)
	{
		src.setContents(new byte[] {'b', 'b'});
		src.setExpires(new Date(2000));
		return src;
	}
}
