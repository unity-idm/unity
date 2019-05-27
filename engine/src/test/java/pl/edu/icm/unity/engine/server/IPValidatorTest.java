/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.Test;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.server.IPValidator.AddressNotAllowedException;

public class IPValidatorTest
{
	@Test
	public void shouldAllowIndividualAddressV4()
	{
		IPValidator validator = new IPValidator(Lists.newArrayList("128.1.1.1", "129.10.0.0"));
		
		Throwable error = catchThrowable(() -> validator.validateIPAddress("128.1.1.1"));
		
		assertThat(error).isNull();
	}


	@Test
	public void shouldAllowAddressInNetworkV4()
	{
		IPValidator validator = new IPValidator(Lists.newArrayList("129.10.0.0/24"));
		
		Throwable error = catchThrowable(() -> validator.validateIPAddress("129.10.0.200"));
		
		assertThat(error).isNull();
	}

	@Test
	public void shouldAllowAddressInWideNetworkV4()
	{
		IPValidator validator = new IPValidator(Lists.newArrayList("129.254.0.0/9"));
		
		Throwable error = catchThrowable(() -> validator.validateIPAddress("129.128.4.4"));
		
		assertThat(error).isNull();
	}

	@Test
	public void shouldAllowAddressWith32bitMaskV4()
	{
		IPValidator validator = new IPValidator(Lists.newArrayList("129.254.0.1/32"));
		
		Throwable error = catchThrowable(() -> validator.validateIPAddress("129.254.0.1"));
		
		assertThat(error).isNull();
	}
	
	@Test
	public void shouldBlockAddressNotInNetworkV4()
	{
		IPValidator validator = new IPValidator(Lists.newArrayList("129.10.0.0/24"));
		
		Throwable error = catchThrowable(() -> validator.validateIPAddress("129.10.1.200"));
		
		assertThat(error).isInstanceOf(AddressNotAllowedException.class);
	}

	@Test
	public void shouldBlockAddressNotIndividualV4()
	{
		IPValidator validator = new IPValidator(Lists.newArrayList("129.10.0.0"));
		
		Throwable error = catchThrowable(() -> validator.validateIPAddress("129.10.0.1"));
		
		assertThat(error).isInstanceOf(AddressNotAllowedException.class);
	}
	

	@Test
	public void shouldAllowAnyAddressWithEmptyWhitelistV4()
	{
		IPValidator validator = new IPValidator(Lists.newArrayList());
		
		Throwable error = catchThrowable(() -> validator.validateIPAddress("129.10.0.1"));
		
		assertThat(error).isNull();
	}
	
	
	
	@Test
	public void shouldAllowIndividualAddressV6()
	{
		IPValidator validator = new IPValidator(Lists.newArrayList("2222:2222:2222:2222:2222:2222:2222:2222"));
		
		Throwable error = catchThrowable(() -> validator.validateIPAddress("2222:2222:2222:2222:2222:2222:2222:2222"));
		
		assertThat(error).isNull();
	}


	@Test
	public void shouldAllowAddressInNetworkV6()
	{
		IPValidator validator = new IPValidator(Lists.newArrayList("2222:2222:2222:2222:2222:2222:2222:2222/64"));
		
		Throwable error = catchThrowable(() -> validator.validateIPAddress("2222:2222:2222:2222:1111:1111:1111:1111"));
		
		assertThat(error).isNull();
	}

	@Test
	public void shouldAllowAddressInWideNetworkV6()
	{
		IPValidator validator = new IPValidator(Lists.newArrayList("2222:2222:2222:2222:2222:2222:2222:2222/4"));
		
		Throwable error = catchThrowable(() -> validator.validateIPAddress("2111:1111:1111:1111:1111:1111:1111:1111"));
		
		assertThat(error).isNull();
	}

	@Test
	public void shouldAllowAddressWith128bitMaskV6()
	{
		IPValidator validator = new IPValidator(Lists.newArrayList("2222:2222:2222:2222:2222:2222:2222:2222/128"));
		
		Throwable error = catchThrowable(() -> validator.validateIPAddress("2222:2222:2222:2222:2222:2222:2222:2222"));
		
		assertThat(error).isNull();
	}
	
	@Test
	public void shouldBlockAddressNotInNetworkV6()
	{
		IPValidator validator = new IPValidator(Lists.newArrayList("2222:2222:2222:2222:2222:2222:2222:2222/64"));
		
		Throwable error = catchThrowable(() -> validator.validateIPAddress("2222:2222:2222:2221:1111:1111:1111:1111"));
		
		assertThat(error).isInstanceOf(AddressNotAllowedException.class);
	}

	@Test
	public void shouldBlockAddressNotIndividualV6()
	{
		IPValidator validator = new IPValidator(Lists.newArrayList("2222:2222:2222:2222:2222:2222:2222:2222"));
		
		Throwable error = catchThrowable(() -> validator.validateIPAddress("2222:2222:2222:2222:2222:2222:2222:2223"));
		
		assertThat(error).isInstanceOf(AddressNotAllowedException.class);
	}
	

	@Test
	public void shouldAllowAnyAddressWithEmptyWhitelistV6()
	{
		IPValidator validator = new IPValidator(Lists.newArrayList());
		
		Throwable error = catchThrowable(() -> validator.validateIPAddress("2222:2222:2222:2222:2222:2222:2222:2223"));
		
		assertThat(error).isNull();
	}
}
