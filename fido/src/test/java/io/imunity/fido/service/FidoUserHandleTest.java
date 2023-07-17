/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.service;


import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link FidoUserHandle} class
 */
public class FidoUserHandleTest
{
	@Test
	public void shouldCreateCopyOfArray()
	{
		//given
		byte[] value = new byte[64];
		new Random().nextBytes(value);

		//when
		FidoUserHandle userHandle = new FidoUserHandle(value);

		//then
		assertThat(value).isEqualTo(userHandle.getBytes());
		// change value
		value[0] = value[0] == 0 ? (byte) 1 : (byte) 0;

		assertThat(value).isNotEqualTo(userHandle.getBytes());
	}

	@Test
	public void shouldCreateRandomUserHandle()
	{
		//given/when
		FidoUserHandle userHandle1 = FidoUserHandle.create();
		FidoUserHandle userHandle2 = FidoUserHandle.create();

		//then
		assertThat(userHandle1.getBytes()).isNotEqualTo(userHandle2.getBytes());
	}

	@Test
	public void shouldConvertIdempotent()
	{
		//given
		FidoUserHandle userHandle = FidoUserHandle.create();

		//when
		String userHandleStr = userHandle.asString();
		FidoUserHandle userHandle1 = FidoUserHandle.fromString(userHandleStr);

		//then
		assertThat(userHandle.getBytes()).isEqualTo(userHandle1.getBytes());
		assertThat(userHandle.asString()).isEqualTo(userHandle1.asString());
	}
}
