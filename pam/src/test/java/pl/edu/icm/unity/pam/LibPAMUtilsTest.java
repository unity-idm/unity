/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.pam;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.engine.api.authn.remote.RemoteAttribute;

public class LibPAMUtilsTest
{
	@Test
	public void shouldParseEmptyGecosWithCommas()
	{
		List<RemoteAttribute> attrs = LibPAMUtils.processGecos(",,,");
		assertThat(attrs).isEmpty();
	}

	@Test
	public void shouldParseGecosWithOnly2ndEntry()
	{
		List<RemoteAttribute> attrs = LibPAMUtils.processGecos(",VAL,,");
		assertThat(attrs).hasSize(1);
		assertThat(attrs.get(0).getName()).isEqualTo("contact");
		assertThat(attrs.get(0).getValues().get(0)).isEqualTo("VAL");
	}
}
