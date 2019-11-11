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
				.withNumberOfThreads(10)
				.withPerformanceTestConfig(PerformanceTestConfig.builder()
					.withUnityURL("https://localhost:2443")
					.withRestUserName("a")
					.withRestUserPasswd("a")
					.build())	
				.withSingleOperationProvider(SingleLoginLogoutOperation::new)
				.build();
		
		testExecutor.run(TimeUnit.SECONDS, 20);
	}
	
	@Test
	public void runOnceSingleLoginLogoutOperation()
	{
		SingleLoginLogoutOperation oper = new SingleLoginLogoutOperation(1, PerformanceTestConfig.builder()
				.withUnityURL("https://localhost:2443")
				.withRestUserName("a")
				.withRestUserPasswd("a")
				.build());
		oper.beforeRun();
		oper.run();
		oper.afterRun();
	}
}
