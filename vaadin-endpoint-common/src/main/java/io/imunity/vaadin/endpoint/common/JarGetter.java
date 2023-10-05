/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JarGetter {
	static List<String> vaadinJars = List.of(
			"flow-client",
			"flow-push",
			"flow-server",
			"vaadin-client-compiled",
			"vaadin-core"
	);

	public static String getJarsRegex(Set<String> classPathElements)
	{
		return vaadinJars.stream().collect(Collectors.joining(".*|.*", "(.*", ".*|")) +
			classPathElements.stream().collect(Collectors.joining("|", "", ")"));
	}
}
