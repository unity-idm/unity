/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import org.junit.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link UserHandle} class
 */
public class UserHandleTest
{
	@Test
	public void shouldCreateCopyOfArray()
	{
		//given
		byte[] value = new byte[64];
		new Random().nextBytes(value);

		//when
		UserHandle userHandle = new UserHandle(value);

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
		UserHandle userHandle1 = UserHandle.create();
		UserHandle userHandle2 = UserHandle.create();

		//then
		assertThat(userHandle1.getBytes()).isNotEqualTo(userHandle2.getBytes());
	}

	@Test
	public void shouldConvertIdempotent()
	{
		//given
		UserHandle userHandle = UserHandle.create();

		//when
		String userHandleStr = userHandle.asString();
		UserHandle userHandle1 = UserHandle.fromString(userHandleStr);

		//then
		assertThat(userHandle.getBytes()).isEqualTo(userHandle1.getBytes());
		assertThat(userHandle.asString()).isEqualTo(userHandle1.asString());
	}
}
