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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.base.internal.TransactionalRunner;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypeBean;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypesMapper;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionalRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
@ActiveProfiles("test-storage-h2")
public class RDBMSTransactionTest
{
	@Autowired @Qualifier(SQLTransactionalRunner.NAME)
	private TransactionalRunner tx;
	
	@Autowired
	private StorageCleaner initDB;
	
	@After
	public void cleanDB()
	{
		initDB.reset();
	}
	
	@Test
	public void rdbmsTransactionIsPersistedAfterCommit()
	{
		tx.runInTransaction(() -> {
			getMapper().create(getObject("n1"));
		});
		
		
		AttributeTypeBean ret = getFromDB("n1");
		assertThat(ret, is(notNullValue()));
	}

	@Test
	public void rdbmsDataIsPersistedInTransaction()
	{
		AttributeTypeBean ret = tx.runInTransactionRet(() -> {
			getMapper().create(getObject("n1"));
			return getMapper().getByName("n1");
		});
		
		assertThat(ret, is(notNullValue()));
	}
	
	@Test
	public void rdbmsTransactionIsDroppedWithoutCommit()
	{
		catchException(tx).runInTransaction(() -> {
			getMapper().create(getObject("n1"));
			throw new RuntimeException("break");
		});
		
		
		AttributeTypeBean ret = getFromDB("n1");
		assertThat(caughtException(), isA(RuntimeException.class));
		assertThat(ret, is(nullValue()));
	}
	
	@Test
	public void rdbmsNoAutoCommitIsHonored()
	{
		tx.runInTransactionNoAutoCommit(() -> {
			getMapper().create(getObject("n1"));
		});
		
		
		AttributeTypeBean ret = getFromDB("n1");
		assertThat(ret, is(nullValue()));
	}

	@Test
	public void rdbmsTransactionWithExceptionIsRetried()
	{
		AtomicInteger i = new AtomicInteger(0);
		
		tx.runInTransaction(() -> {
			if (i.incrementAndGet() < 3)
				throw new PersistenceException("test");
			getMapper().create(getObject("n1"));
		});
		
		
		AttributeTypeBean ret = getFromDB("n1");
		assertThat(ret, is(notNullValue()));
	}
	
	@Test
	public void rdbmsNestedTransactionIsIncludedWithMainCommit()
	{
		tx.runInTransaction(() -> {
			getMapper().create(getObject("n1"));
			tx.runInTransaction(() -> {
				getMapper().create(getObject("n2"));
			});
		});
		
		
		AttributeTypeBean ret1 = getFromDB("n1");
		AttributeTypeBean ret2 = getFromDB("n2");
		
		assertThat(ret1, is(notNullValue()));
		assertThat(ret2, is(notNullValue()));
	}
	
	@Test
	public void rdbmsTransactionWithNestedIsDroppedWithoutCommit()
	{
		tx.runInTransactionNoAutoCommit(() -> {
			getMapper().create(getObject("n1"));
			tx.runInTransactionNoAutoCommit(() -> {
				getMapper().create(getObject("n2"));
			});
		});
		
		
		AttributeTypeBean ret1 = getFromDB("n1");
		AttributeTypeBean ret2 = getFromDB("n2");
		
		assertThat(ret1, is(nullValue()));
		assertThat(ret2, is(nullValue()));
	}
	
	@Test
	public void rdbmsNestedTransactionExceptionBreaksAllTransactions()
	{
		catchException(tx).runInTransaction(() -> {
			getMapper().create(getObject("n1"));
			tx.runInTransactionNoAutoCommit(() -> {
				throw new RuntimeException("test");
			});
		});
		
		
		AttributeTypeBean ret1 = getFromDB("n1");
		
		assertThat(ret1, is(nullValue()));
		assertThat(caughtException(), isA(RuntimeException.class));
	}

	@Test
	public void rdbmsNestedTransactionWithExceptionIsRetried()
	{
		AtomicInteger i = new AtomicInteger(0);
		
		tx.runInTransaction(() -> {
			getMapper().create(getObject("n1"));
			tx.runInTransaction(() -> {
				if (i.incrementAndGet() < 3)
					throw new PersistenceException("test");
				getMapper().create(getObject("n2"));
			});
		});
		
		
		AttributeTypeBean ret = getFromDB("n1");
		assertThat(ret, is(notNullValue()));
		
		AttributeTypeBean ret2 = getFromDB("n2");
		assertThat(ret2, is(notNullValue()));
	}

	@Test
	public void rdbmsManualCommitIsApplied()
	{
		tx.runInTransactionNoAutoCommit(() -> {
			getMapper().create(getObject("n1"));
			SQLTransactionTL.getSql().commit();
			getMapper().create(getObject("n2"));
		});
		
		
		AttributeTypeBean ret = getFromDB("n1");
		assertThat(ret, is(notNullValue()));
		
		AttributeTypeBean ret2 = getFromDB("n2");
		assertThat(ret2, is(nullValue()));
	}
	
	private AttributeTypeBean getFromDB(String name)
	{
		return tx.runInTransactionRet(() -> {
			return getMapper().getByName(name);
		});
	}
	
	private AttributeTypesMapper getMapper()
	{
		return SQLTransactionTL.getSql().getMapper(AttributeTypesMapper.class);
	}
	
	protected AttributeTypeBean getObject(String name)
	{
		byte[] contents = new byte[] {'1', '2'};
		AttributeTypeBean bean = new AttributeTypeBean(name, contents, "syntax");
		return bean;
	}

}
