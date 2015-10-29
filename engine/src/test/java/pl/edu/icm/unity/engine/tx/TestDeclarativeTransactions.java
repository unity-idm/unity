/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.tx;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.apache.ibatis.session.SqlSession;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.transactions.Propagation;
import pl.edu.icm.unity.engine.transactions.SqlSessionTL;
import pl.edu.icm.unity.engine.transactions.Transactional;


public class TestDeclarativeTransactions extends DBIntegrationTestBase
{
	@Autowired
	private TransactionCall checkSessionWithInterface;
	@Autowired
	private CheckPropagationParent checkPropagationParent;
	@Autowired
	private CheckSessionWithoutInterface checkSessionWithoutInterface;

	
	@Test
	public void requireSeparateTransactionIsStarted()
	{
		checkSessionWithInterface.requireSeparate();
	}

	@Test
	public void requiredTransactionIsStarted()
	{
		checkSessionWithInterface.required();
	}

	@Test
	public void requiredTransactionIsPropagatedToChildren()
	{
		checkPropagationParent.required();
	}

	@Test
	public void requireSeparateTransactionGetsNewTransationInChildInvocation()
	{
		checkPropagationParent.requiredChildRequiresNew();
	}
	
	@Test
	public void transactionIsStartedOnClassWithoutInterface()
	{
		checkSessionWithoutInterface.required();
	}
	
	@Component
	public static class CheckSessionWithInterface implements TransactionCall
	{
		@Transactional
		@Override
		public void required()
		{
			SqlSession sql = SqlSessionTL.get();
			assertThat(sql, notNullValue());
		}

		@Transactional(propagation=Propagation.REQUIRE_SEPARATE)
		@Override
		public void requireSeparate()
		{
			SqlSession sql = SqlSessionTL.get();
			assertThat(sql, notNullValue());
		}
	}
	
	@Component
	public static class CheckSessionWithoutInterface
	{
		@Transactional
		public void required()
		{
			SqlSession sql = SqlSessionTL.get();
			assertThat(sql, notNullValue());
		}
	}

	@Component
	public static class CheckPropagationParent
	{
		@Autowired
		private CheckPropagationChild checkPropagationChild;
		
		@Transactional
		public void required()
		{
			SqlSession sql = SqlSessionTL.get();
			checkPropagationChild.required(sql);
		}

		@Transactional
		public void requiredChildRequiresNew()
		{
			SqlSession sql = SqlSessionTL.get();
			checkPropagationChild.requireSeparate(sql);
		}
	}

	@Component
	public static class CheckPropagationChild
	{
		@Transactional
		public void required(SqlSession parentsSession)
		{
			SqlSession sql = SqlSessionTL.get();
			assertThat(sql, notNullValue());
			assertThat(sql, equalTo(parentsSession));
		}

		@Transactional(propagation=Propagation.REQUIRE_SEPARATE)
		public void requireSeparate(SqlSession parentsSession)
		{
			SqlSession sql = SqlSessionTL.get();
			assertThat(sql, notNullValue());
			assertThat(sql, not(equalTo(parentsSession)));
		}
	}
	
	public interface TransactionCall
	{
		void required();
		void requireSeparate();
	}
}
