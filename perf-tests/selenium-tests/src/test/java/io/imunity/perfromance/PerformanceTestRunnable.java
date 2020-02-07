/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.perfromance;

public interface PerformanceTestRunnable extends Runnable
{
	void beforeRun();
	
	void afterRun();
	
	default void reset()
	{
		afterRun();
		beforeRun();
	}
	
	String takeScreenshot(String suffix);
}
