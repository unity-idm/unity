/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.facilities;

import java.util.Collections;
import java.util.List;

class AuthenticationFlowEntry
{
	final AuthenticationFlowDefinitionForBinder flow;
	final List<String> endpoints;

	AuthenticationFlowEntry(AuthenticationFlowDefinitionForBinder flow, List<String> endpoints)
	{
		this.flow = flow;
		this.endpoints = Collections.unmodifiableList(endpoints == null ? Collections.emptyList() : endpoints);
	}
}
