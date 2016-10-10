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

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.hazelcast.core.HazelcastException;
import com.hazelcast.core.TransactionalMap;

import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.hz.tx.HzTransactionTL;
import pl.edu.icm.unity.store.hz.tx.HzTransactionalRunner;
import pl.edu.icm.unity.store.tx.TransactionTL;

/**
 * Note: this test is only run on HZ storage engine
 * @author K. Benedyczak
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
public class HzTransactionTest
{
	@Autowired @Qualifier(HzTransactionalRunner.NAME)
	private TransactionalRunner tx;
	
	@Autowired
	private StorageCleanerImpl initDB;
	
	@Autowired
	private StorageConfiguration systemCfg;
	
	@Before
	public void beforeMethod() 
	{
		StorageEngine engine = systemCfg.getEnumValue(StorageConfiguration.ENGINE, StorageEngine.class);
		Assume.assumeTrue(engine == StorageEngine.hz);
	}

	@After
	public void cleanDB()
	{
		initDB.reset();
	}
	
	@Test
	public void transactionIsPersistedAfterCommit()
	{
		tx.runInTransaction(() -> {
			getMap().put("n1", "v1");
		});
		
		
		String ret = getFromStore("n1");
		assertThat(ret, is(notNullValue()));
	}

	@Test
	public void dataIsPersistedInTransaction()
	{
		String ret = tx.runInTransactionRet(() -> {
			getMap().put("n1", "v1");
			return getMap().get("n1");
		});
		
		assertThat(ret, is(notNullValue()));
	}
	
	@Test
	public void transactionIsDroppedWithoutCommit()
	{
		catchException(tx).runInTransaction(() -> {
			getMap().put("n1", "v1");
			throw new RuntimeException("break");
		});
		
		
		String ret = getFromStore("n1");
		assertThat(caughtException(), isA(RuntimeException.class));
		assertThat(ret, is(nullValue()));
	}
	
	@Test
	public void noAutoCommitIsHonored()
	{
		tx.runInTransactionNoAutoCommit(() -> {
			getMap().put("n1", "v1");
		});
		
		
		String ret = getFromStore("n1");
		assertThat(ret, is(nullValue()));
	}

	@Test
	public void transactionWithExceptionIsRetried()
	{
		AtomicInteger i = new AtomicInteger(0);
		
		tx.runInTransaction(() -> {
			if (i.incrementAndGet() < 3)
				throw new HazelcastException("test");
			getMap().put("n1", "v1");
		});
		
		
		String ret = getFromStore("n1");
		assertThat(ret, is(notNullValue()));
	}
	
	@Test
	public void nestedTransactionIsIncludedWithMainCommit()
	{
		tx.runInTransaction(() -> {
			getMap().put("n1", "v1");
			tx.runInTransaction(() -> {
				getMap().put("n2", "v2");
			});
		});
		
		
		String ret1 = getFromStore("n1");
		String ret2 = getFromStore("n2");
		
		assertThat(ret1, is(notNullValue()));
		assertThat(ret2, is(notNullValue()));
	}
	
	@Test
	public void transactionWithNestedIsDroppedWithoutCommit()
	{
		tx.runInTransactionNoAutoCommit(() -> {
			getMap().put("n1", "v1");
			tx.runInTransactionNoAutoCommit(() -> {
				getMap().put("n2", "v2");
			});
		});
		
		
		String ret1 = getFromStore("n1");
		String ret2 = getFromStore("n2");
		
		assertThat(ret1, is(nullValue()));
		assertThat(ret2, is(nullValue()));
	}
	
	@Test
	public void nestedTransactionExceptionBreaksAllTransactions()
	{
		catchException(tx).runInTransaction(() -> {
			getMap().put("n1", "v1");
			tx.runInTransactionNoAutoCommit(() -> {
				throw new RuntimeException("test");
			});
		});
		
		
		String ret1 = getFromStore("n1");
		
		assertThat(ret1, is(nullValue()));
		assertThat(caughtException(), isA(RuntimeException.class));
	}

	@Test
	public void nestedTransactionWithExceptionIsRetried()
	{
		AtomicInteger i = new AtomicInteger(0);
		
		tx.runInTransaction(() -> {
			getMap().put("n1", "v1");
			tx.runInTransaction(() -> {
				if (i.incrementAndGet() < 3)
					throw new HazelcastException("test");
				getMap().put("n2", "v2");
			});
		});
		
		
		String ret = getFromStore("n1");
		assertThat(ret, is(notNullValue()));
		
		String ret2 = getFromStore("n2");
		assertThat(ret2, is(notNullValue()));
	}

	@Test
	public void manualCommitIsApplied()
	{
		tx.runInTransactionNoAutoCommit(() -> {
			getMap().put("n1", "v1");
			TransactionTL.manualCommit();
			getMap().put("n2", "v2");
		});
		
		
		String ret = getFromStore("n1");
		assertThat(ret, is(notNullValue()));
		
		String ret2 = getFromStore("n2");
		assertThat(ret2, is(nullValue()));
	}
	
	private String getFromStore(String name)
	{
		return tx.runInTransactionRet(() -> {
			return getMap().get(name);
		});
	}
	
	protected TransactionalMap<String, String> getMap()
	{
		return HzTransactionTL.getHzContext().getMap("testStore");
	}
}
