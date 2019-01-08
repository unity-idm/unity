/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.pam;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

import pl.edu.icm.unity.engine.api.authn.remote.RemoteAttribute;

public class LibPAMUtilsTest
{
	@Test
	public void shouldParseEmptyGecosWithCommas()
	{
		List<RemoteAttribute> attrs = LibPAMUtils.processGecos(",,,");
		assertThat(attrs.isEmpty(), is(true));
	}

	@Test
	public void shouldParseGecosWithOnly2ndEntry()
	{
		List<RemoteAttribute> attrs = LibPAMUtils.processGecos(",VAL,,");
		assertThat(attrs.size(), is(1));
		assertThat(attrs.get(0).getName(), is("contact"));
		assertThat(attrs.get(0).getValues().get(0), is("VAL"));
	}
}
