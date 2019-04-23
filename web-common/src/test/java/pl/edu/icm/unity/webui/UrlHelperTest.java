/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

public class UrlHelperTest
{
	@Test
	public void shouldPreserveURLEncondingInRelativeURL() throws URISyntaxException
	{
		String result = UrlHelper.getRelativeURIFrom(new URI(
				"https://some.host:2443/path%2Fwith?arg=val%7Cpipe#%7C-morepipe"));
		
		assertThat(result).isEqualTo("/path%2Fwith?arg=val%7Cpipe#%7C-morepipe");
	}
}
