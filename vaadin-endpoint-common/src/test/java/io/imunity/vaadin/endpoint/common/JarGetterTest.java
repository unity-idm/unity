/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class JarGetterTest
{
	@Test
	void shouldIncludeLumoThemeJar()
	{
		// given
		String lumoThemeJar = "/maven/repository/com/vaadin/vaadin-lumo-theme-25.0.13.jar";

		// when
		String jarsRegex = JarGetter.getJarsRegex(Set.of());

		// then
		assertThat(lumoThemeJar).matches(jarsRegex);
	}
}
