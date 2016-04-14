/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.transaction.HazelcastXAResource;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.api.tx.Propagation;
import pl.edu.icm.unity.store.rdbms.DBSessionManager;

public class TransactionState
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, TransactionState.class);
	private Propagation propagation;
	private SqlSession sqlSession;
	private HazelcastXAResource hzXAResource;
	private DBSessionManager dbSessionMan;
	private HazelcastInstance hzInstance;
	private TransactionManager transactionManager;
	
	public TransactionState(Propagation propagation, DBSessionManager dbSessionMan, 
			HazelcastInstance hzInstance, TransactionManager transactionManager)
	{
		this.propagation = propagation;
		this.dbSessionMan = dbSessionMan;
		this.hzInstance = hzInstance;
		this.transactionManager = transactionManager;
	}
	
	public TransactionState(Propagation propagation, TransactionState parent)
	{
		this.propagation = propagation;
		this.dbSessionMan = parent.dbSessionMan;
		this.hzInstance = parent.hzInstance;
		this.transactionManager = parent.transactionManager;
		if (propagation != Propagation.REQUIRE_SEPARATE)
			this.hzXAResource = parent.hzXAResource;
		this.sqlSession = parent.sqlSession;
	}
	
	public Propagation getPropagation()
	{
		return propagation;
	}
	public SqlSession getSqlSession()
	{
		if (sqlSession == null)
		{
			log.trace("Adding RDBMS to transaction");
			sqlSession = dbSessionMan.getSqlSession(true);
		}
		return sqlSession;
	}
	public HazelcastXAResource getHzXAResource()
	{
		if (hzXAResource == null)
			addHzToTransaction();
		return hzXAResource;
	}

	private void addHzToTransaction()
	{
		try
		{
			log.trace("Adding Hazelcast to transaction");
			Transaction transaction = transactionManager.getTransaction();
			hzXAResource = hzInstance.getXAResource();
			transaction.enlistResource(hzXAResource);
		} catch (SystemException | RollbackException e)
		{
			throw new IllegalStateException("Can't enlist "
					+ "Hazelcast resource to transaction", e);
		}
	}
	
	/**
	 * Must be called before each and every commit or rollback of a transaction,
	 * including nested
	 */
	public void delist()
	{
		//FIXME - is this flag correct?
		if (hzXAResource != null)
		{
			log.trace("Delisting Hazelcast from transaction");
			try
			{
				transactionManager.getTransaction().delistResource(hzXAResource, 
					XAResource.TMSUCCESS);
			} catch (SystemException e)
			{
				throw new IllegalStateException("Can't delist "
						+ "Hazelcast resource from transaction", e);
			}
		}
	}
	
	/**
	 * Must be called after commit or rollback of the top-most transaction.
	 */
	public void close()
	{
		if (sqlSession != null)
		{
			log.trace("Closing RDBMS session");
			sqlSession.close();
		}
	}
}