/**********************************************************************
 *                     Copyright (c) 2019, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package io.imunity.perfromance;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class PerformanceTest
{
	@Test
	public void loginLogoutTest() throws InterruptedException
	{
		PerformanceTestExecutor testExecutor = PerformanceTestExecutor.builder()
				.withNumberOfThreads(1)
				.withSingleOperationSupplier(SingleLoginLogoutOperation::new)
				.build();
		
		testExecutor.run(TimeUnit.SECONDS, 5);
	}
	
	@Test
	public void runOnceSingleLoginLogoutOperation()
	{
		SingleLoginLogoutOperation oper = new SingleLoginLogoutOperation();
		oper.beforeRun();
		oper.run();
		oper.afterRun();
	}
}
