/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.perfromance;

public interface PerformanceTestProvider
{
	PerformanceTestRunnable get(int idx, PerformanceTestConfig config);
}
