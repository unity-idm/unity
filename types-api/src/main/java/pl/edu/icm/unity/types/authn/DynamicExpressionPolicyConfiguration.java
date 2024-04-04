/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.authn;

import java.util.Objects;

public class DynamicExpressionPolicyConfiguration implements AuthenticationPolicyConfiguration
{
	public final String mvelExpression;

	public DynamicExpressionPolicyConfiguration(String mvelExpression)
	{
		this.mvelExpression = mvelExpression;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(mvelExpression);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DynamicExpressionPolicyConfiguration other = (DynamicExpressionPolicyConfiguration) obj;
		return Objects.equals(mvelExpression, other.mvelExpression);
	}
	
	
}
