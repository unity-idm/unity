/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.mvel;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import pl.edu.icm.unity.types.translation.ActionParameterDefinitionDetails;

public class MVELExpressionContext implements  ActionParameterDefinitionDetails
{
	public final String titleKey;
	public final String evalToKey;
	public final Map<String, String> vars;

	private MVELExpressionContext(Builder builder)
	{
		this.titleKey = builder.titleKey;
		this.evalToKey = builder.evalToKey;
		this.vars = builder.vars;
	}

	public MVELExpressionContext(String title, String evalTo, Map<String, String> vars)
	{
		this.titleKey = title;
		this.evalToKey = evalTo;
		this.vars = ImmutableMap.copyOf(vars);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private String titleKey;
		private String evalToKey;
		private Map<String, String> vars = Collections.emptyMap();

		private Builder()
		{
		}

		public Builder withTitleKey(String title)
		{
			this.titleKey = title;
			return this;
		}

		public Builder withEvalToKey(String evalTo)
		{
			this.evalToKey = evalTo;
			return this;
		}

		public Builder withVars(Map<String, String> vars)
		{
			this.vars = vars;
			return this;
		}

		public Builder withVar(String name, String value)
		{
			this.vars.put(name, value);
			return this;
		}

		public MVELExpressionContext build()
		{
			return new MVELExpressionContext(this);
		}
	}
}
