/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.mvel;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import org.mvel2.MVEL;

public class MVELCompiledExpressionsCache
{
	private static final Map<ExpressionString, Serializable> CACHE = new WeakHashMap<>();
	
	public static Serializable getCompiledExpression(String expression)
	{
		return CACHE.computeIfAbsent(new ExpressionString(expression), 
				expr -> MVEL.compileExpression(expr.expression));
	}
	
	private static class ExpressionString
	{
		private final String expression;

		public ExpressionString(String expression)
		{
			this.expression = expression;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(expression);
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
			ExpressionString other = (ExpressionString) obj;
			return Objects.equals(expression, other.expression);
		}
	}
}
