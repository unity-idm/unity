package pl.edu.icm.unity.oauth.as;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.logging.log4j.Logger;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import pl.edu.icm.unity.base.utils.Log;

public record ActiveOAuthScopeDefinition(
		String name,
		String description,
		List<String> attributes,
		boolean wildcard)
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, ActiveOAuthScopeDefinition.class);

	public ActiveOAuthScopeDefinition
	{
		attributes = Optional.ofNullable(attributes).map(List::copyOf).orElse(List.of());
	}

	public boolean match(String scope, boolean allowForRequestingWildcard)
	{
		if (!wildcard)
			return name.equals(scope);

		if (!allowForRequestingWildcard)
		{
			try
			{
				return Pattern.matches(name, scope);
			} catch (PatternSyntaxException e)
			{
				log.error("Incorrect pattern", e);
				return false;
			}
		} else
		{
			return isSubsetOfWildcardScope(scope, name);
		}
	}

	private boolean isSubsetOfWildcardScope(String wildcard1, String wildcard2)
	{
		Automaton a1 = new RegExp(wildcard1).toAutomaton();
		Automaton a2 = new RegExp(wildcard2).toAutomaton();
		return a1.minus(a2)
				.isEmpty();
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String name;
		private String description;
		private List<String> attributes = Collections.emptyList();
		private boolean wildcard;

		private Builder()
		{
		}

		public Builder withName(String name)
		{
			this.name = name;
			return this;
		}

		public Builder withDescription(String description)
		{
			this.description = description;
			return this;
		}

		public Builder withAttributes(List<String> attributes)
		{
			this.attributes = attributes;
			return this;
		}

		public Builder withWildcard(boolean wildcard)
		{
			this.wildcard = wildcard;
			return this;
		}

		public ActiveOAuthScopeDefinition build()
		{
			return new ActiveOAuthScopeDefinition(name, description, attributes, wildcard);
		}
	}
}