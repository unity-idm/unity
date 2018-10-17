/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation;

/**
 * Contains additional data related with @{TranslationRuleInstance} invocation.
 * 
 * @author P.Piernik
 *
 */
public class TranslationRuleInvocationContext
{
	private String includedProfile;

	public TranslationRuleInvocationContext()
	{
	}

	public void setIncludedProfile(String profile)
	{
		includedProfile = profile;
	}

	public String getIncludedProfile()
	{
		return includedProfile;
	}
}
