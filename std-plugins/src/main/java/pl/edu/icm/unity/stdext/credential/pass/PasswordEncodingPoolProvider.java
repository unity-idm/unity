/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.pass;

import java.util.concurrent.ForkJoinPool;

/**
 * Provides access to a shared thread pool on which all password hashings should be performed.
 * This pool size allows for a global control of concurrent password hashings.   
 */
public class PasswordEncodingPoolProvider
{
	public final ForkJoinPool pool;

	public PasswordEncodingPoolProvider(ForkJoinPool pool)
	{
		this.pool = pool;
	}
}
