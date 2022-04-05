/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.perfromance;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

public class PerformanceTest
{
	@Before
	public void before()
	{
		// FIXME: this should be set in env, not test
		System.setProperty("webdriver.chrome.driver", "/opt/homebrew/bin/chromedriver");
	}
	
	
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
