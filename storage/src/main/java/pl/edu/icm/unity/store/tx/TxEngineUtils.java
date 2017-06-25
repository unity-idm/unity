/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.tx;

/**
 * Helpers for transaction engine implementations.
 * @author K. Benedyczak
 */
public class TxEngineUtils
{
	public static void sleepInterruptible(int retryNum, long retryBaseDelay, long retryMaxDelay)
	{
		long ms = retryNum * retryBaseDelay;
		if (ms > retryMaxDelay)
			ms = retryMaxDelay;
		try
		{
			Thread.sleep(ms);
		} catch (InterruptedException e)
		{
			//ok
		}
	}
}
