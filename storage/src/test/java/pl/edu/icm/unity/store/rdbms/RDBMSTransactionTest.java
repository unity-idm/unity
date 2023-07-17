/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import pl.edu.icm.unity.store.StorageCleanerImpl;
import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.StorageEngine;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypeBean;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypesMapper;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionalRunner;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
public class RDBMSTransactionTest
{
	@Autowired @Qualifier(SQLTransactionalRunner.NAME)
	private TransactionalRunner tx;
	
	@Autowired
	private StorageCleanerImpl initDB;
	
	@Autowired
	private StorageConfiguration systemCfg;
	
	@BeforeEach
	public void conditionalStart()
	{
		StorageEngine engine = systemCfg.getEnumValue(StorageConfiguration.ENGINE, StorageEngine.class);
		assertThat(engine).isEqualTo(StorageEngine.rdbms);
	}
	
	@AfterEach
	public void cleanDB()
	{
		initDB.cleanOrDelete();
	}
	
	@Test
	public void rdbmsTransactionIsPersistedAfterCommit()
	{
		tx.runInTransaction(() -> {
			getMapper().create(getObject("n1"));
		});
		
		
		AttributeTypeBean ret = getFromDB("n1");
		assertThat(ret).isNotNull();
	}

	@Test
	public void rdbmsDataIsPersistedInTransaction()
	{
		AttributeTypeBean ret = tx.runInTransactionRet(() -> {
			getMapper().create(getObject("n1"));
			return getMapper().getByName("n1");
		});
		
		assertThat(ret).isNotNull();
	}
	
	@Test
	public void rdbmsTransactionIsDroppedWithoutCommit()
	{
		Throwable error = catchThrowable(() -> tx.runInTransaction(() -> {
			getMapper().create(getObject("n1"));
			throw new RuntimeException("break");
		}));
		
		
		AttributeTypeBean ret = getFromDB("n1");
		assertThat(error).isInstanceOf(RuntimeException.class);
		assertThat(ret).isNull();
	}
	
	@Test
	public void rdbmsNoAutoCommitIsHonored()
	{
		tx.runInTransactionNoAutoCommit(() -> {
			getMapper().create(getObject("n1"));
		});
		
		
		AttributeTypeBean ret = getFromDB("n1");
		assertThat(ret).isNull();
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
		assertThat(ret).isNotNull();
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
		
		assertThat(ret1).isNotNull();
		assertThat(ret2).isNotNull();
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
		
		assertThat(ret1).isNull();
		assertThat(ret2).isNull();
	}
	
	@Test
	public void rdbmsNestedTransactionExceptionBreaksAllTransactions()
	{
		Throwable error = catchThrowable(() -> tx.runInTransaction(() -> {
			getMapper().create(getObject("n1"));
			tx.runInTransactionNoAutoCommit(() -> {
				throw new RuntimeException("test");
			});
		}));
		
		
		AttributeTypeBean ret1 = getFromDB("n1");
		
		assertThat(ret1).isNull();
		assertThat(error).isInstanceOf(RuntimeException.class);
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
		assertThat(ret).isNotNull();
		
		AttributeTypeBean ret2 = getFromDB("n2");
		assertThat(ret2).isNotNull();
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
		assertThat(ret).isNotNull();
		
		AttributeTypeBean ret2 = getFromDB("n2");
		assertThat(ret2).isNull();
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
