/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.registration;

import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class CredentialParamValueTest
{

	@Test
	public void shouldNotPrintSecrets()
	{
		// given
		String uuid = UUID.randomUUID().toString();
		CredentialParamValue paramValue = new CredentialParamValue("id", uuid);
		
		// when
		String toStringValue = paramValue.toString();
		
		// then
		Assertions.assertThat(toStringValue).doesNotContain(uuid);
	}

}
