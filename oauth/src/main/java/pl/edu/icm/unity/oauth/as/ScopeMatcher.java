/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.logging.log4j.Logger;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import pl.edu.icm.unity.base.utils.Log;

public class ScopeMatcher
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, ScopeMatcher.class);

	
	public static boolean match(ActiveOAuthScopeDefinition scopeDef,  String scope, boolean supportPattern)
	{
		if (!scopeDef.pattern())
			return scopeDef.name().equals(scope);

		if (!supportPattern)
		{
			try
			{
				return Pattern.matches(scopeDef.name(), scope);
			} catch (PatternSyntaxException e)
			{
				log.error("Incorrect pattern", e);
				return false;
			}
		} else
		{
			return isSubsetOfPatternScope(scope, scopeDef.name());
		}
	}

	public static boolean isSubsetOfPatternScope(String regExp1, String regExp2)
	{
		Automaton a1 = new RegExp(regExp1).toAutomaton();
		Automaton a2 = new RegExp(regExp2).toAutomaton();
		return a1.minus(a2)
				.isEmpty();
	}
}
