/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.flows;

import java.util.Collections;
import java.util.List;

/**
 * 
 * @author P.Piernik
 *
 */
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
