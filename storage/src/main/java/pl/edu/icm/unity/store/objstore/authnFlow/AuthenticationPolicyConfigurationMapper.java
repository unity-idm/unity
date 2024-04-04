/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.authnFlow;

import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.base.authn.AuthenticationPolicyConfiguration;
import pl.edu.icm.unity.base.authn.DynamicExpressionPolicyConfiguration;

class AuthenticationPolicyConfigurationMapper
{
	static String map(AuthenticationPolicyConfiguration config)
	{
		if (config instanceof DynamicExpressionPolicyConfiguration)
		{
			return ((DynamicExpressionPolicyConfiguration) config).mvelExpression;
		}
		
		return null;
		
	}
	
	
	
	static AuthenticationPolicyConfiguration map(Policy policy, String config)
	{
		if (policy.equals(Policy.DYNAMIC_EXPRESSION))
		{
			return new DynamicExpressionPolicyConfiguration(config);
		}
		
		return AuthenticationFlowDefinition.EMPTY_CONFIGURATION;
	}
}
