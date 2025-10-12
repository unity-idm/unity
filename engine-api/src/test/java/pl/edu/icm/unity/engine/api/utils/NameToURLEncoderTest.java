/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;

import org.junit.jupiter.api.Test;

public class NameToURLEncoderTest
{
	@Test
	public void shouldNotEncodeSafeName()
	{
		assertThat(NameToURLEncoder.encode("name")).isEqualTo("name");
	}

	@Test
	public void shouldEncodeUnsafeName()
	{
		assertThat(NameToURLEncoder.encode("/name")).isEqualTo(new String(Base64.getUrlEncoder()
				.encode("/name".getBytes())) + NameToURLEncoder.ENCODED_NAME_SUFFIX);
	}
	
	@Test
	public void shouldDecodeSafeName()
	{
		assertThat(NameToURLEncoder.decode("name")).isEqualTo("name");
	}

	@Test
	public void shouldDecodeUnsafeName()
	{
		assertThat(NameToURLEncoder.decode(new String(Base64.getUrlEncoder()
				.encode("/name".getBytes())) + NameToURLEncoder.ENCODED_NAME_SUFFIX)).isEqualTo("/name");
	}
}
