/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.authnFlow;

import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;

class AuthenticationFlowMapper
{
	static DBAuthenticationFlow map(AuthenticationFlowDefinition authenticationFlowDefinition)
	{
		return DBAuthenticationFlow.builder()
				.withFirstFactorAuthenticators(authenticationFlowDefinition.getFirstFactorAuthenticators())
				.withSecondFactorAuthenticators(authenticationFlowDefinition.getSecondFactorAuthenticators())
				.withName(authenticationFlowDefinition.getName())
				.withPolicy(authenticationFlowDefinition.getPolicy()
						.name())
				.withRevision(authenticationFlowDefinition.getRevision())
				.build();

	}

	static AuthenticationFlowDefinition map(DBAuthenticationFlow dbAuthenticationFlow)
	{
		AuthenticationFlowDefinition authenticationFlowDefinition = new AuthenticationFlowDefinition(
				dbAuthenticationFlow.name, Policy.valueOf(dbAuthenticationFlow.policy),
				dbAuthenticationFlow.firstFactorAuthenticators, dbAuthenticationFlow.secondFactorAuthenticators);
		authenticationFlowDefinition.setRevision(dbAuthenticationFlow.revision);
		return authenticationFlowDefinition;
	}
}
