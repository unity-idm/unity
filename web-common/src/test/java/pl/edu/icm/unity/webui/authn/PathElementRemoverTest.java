/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn;

import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class PathElementRemoverTest
{
	@Test
	public void shouldRemoveEverythingUntilVaadin()
	{
		String cleanedPath = AuthenticationFilter.removePathElementsUntil("console/console/VAADIN/1/2/3.js", Set.of("VAADIN"));
		assertThat(cleanedPath).isEqualTo("/VAADIN/1/2/3.js");
	}

	@Test
	public void shouldRemoveEverythingUntilAPP()
	{
		String cleanedPath = AuthenticationFilter.removePathElementsUntil("abc/11/APP/zz/VAADIN/1/2/3.js", Set.of("APP", "VAADIN"));
		assertThat(cleanedPath).isEqualTo("/APP/zz/VAADIN/1/2/3.js");
	}

	@Test
	public void shouldRemoveEverything()
	{
		String cleanedPath = AuthenticationFilter.removePathElementsUntil("abc/11/APP/zz/ala/1/2/3", Set.of("VAADIN"));
		assertThat(cleanedPath).isEqualTo("");
	}

	@Test
	public void shouldRemoveNothing()
	{
		String cleanedPath = AuthenticationFilter.removePathElementsUntil("/APP/zz/ala/1/2/3", Set.of("APP"));
		assertThat(cleanedPath).isEqualTo("/APP/zz/ala/1/2/3");
	}
}
