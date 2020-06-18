/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.pass;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.ForkJoinPool;

import org.junit.Test;

public class ScrytpEncoderTest
{
	@Test
	public void shouldComputeMaxWorkFactorFor1G_p4()
	{
		SCryptEncoder encoder = new SCryptEncoder(new ForkJoinPool(4), 1024l*1024*1024 + 500_000_000l);
		
		int maxAllowedWorkFactor = encoder.getMaxAllowedWorkFactor();
		
		assertThat(maxAllowedWorkFactor, is(18));
	}

	@Test
	public void shouldComputeMaxWorkFactorFor1G_p1()
	{
		SCryptEncoder encoder = new SCryptEncoder(new ForkJoinPool(1), 1024l*1024*1024 + 500_000_000l);
		
		int maxAllowedWorkFactor = encoder.getMaxAllowedWorkFactor();
		
		assertThat(maxAllowedWorkFactor, is(20));
	}


	@Test
	public void shouldComputeMaxWorkFactorFor25G_p4()
	{
		SCryptEncoder encoder = new SCryptEncoder(new ForkJoinPool(4), 2l*1024*1024*1024 + 500_000_000l);
		
		int maxAllowedWorkFactor = encoder.getMaxAllowedWorkFactor();
		
		assertThat(maxAllowedWorkFactor, is(19));
	}

	@Test
	public void shouldComputeMaxWorkFactorFor25G_p1()
	{
		SCryptEncoder encoder = new SCryptEncoder(new ForkJoinPool(1), 2l*1024*1024*1024 + 500_000_000l);
		
		int maxAllowedWorkFactor = encoder.getMaxAllowedWorkFactor();
		
		assertThat(maxAllowedWorkFactor, is(21));
	}

	
	@Test
	public void shouldReturnMinWorkFactorFor250M_p1()
	{
		SCryptEncoder encoder = new SCryptEncoder(new ForkJoinPool(1), 250l*1024*1024);
		
		int maxAllowedWorkFactor = encoder.getMaxAllowedWorkFactor();
		
		assertThat(maxAllowedWorkFactor, is(ScryptParams.MIN_WORK_FACTOR));
	}
}
