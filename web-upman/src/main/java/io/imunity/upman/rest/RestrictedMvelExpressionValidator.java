/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import java.util.Locale;
import java.util.Set;

/**
 * Accepts only a side-effect free subset of MVEL, so that project managers can not execute arbitrary code
 * through form automation. Allowed are: context variables, literals, indexing, property access,
 * comparison, logical and arithmetic operators and the contains operator. Method calls, class references,
 * object creation, assignments and statements are rejected.
 */
class RestrictedMvelExpressionValidator
{
	private static final Set<String> KEYWORDS = Set.of("true", "false", "null", "nil", "empty", "contains");
	private static final Set<String> FORBIDDEN_PROPERTIES = Set.of("wait", "notify", "notifyall");
	private static final Set<String> TWO_CHAR_OPERATORS = Set.of("==", "!=", "<=", ">=", "&&", "||");
	private static final String SINGLE_CHAR_OPERATORS = "<>!+-*/%";
	private static final String BRACKETS_AND_SEPARATORS = "()[],";

	private final Set<String> allowedVariables;

	RestrictedMvelExpressionValidator(Set<String> allowedVariables)
	{
		this.allowedVariables = Set.copyOf(allowedVariables);
	}

	void validate(String expression, String usage)
	{
		if (expression == null || expression.isBlank())
			return;
		new ExpressionScanner(expression, usage).scan();
	}

	private enum TokenKind
	{
		NONE, IDENTIFIER, CLOSING_BRACKET, DOT, OTHER
	}

	private class ExpressionScanner
	{
		private final String expression;
		private final String usage;
		private int position;
		private TokenKind previous = TokenKind.NONE;

		ExpressionScanner(String expression, String usage)
		{
			this.expression = expression;
			this.usage = usage;
		}

		void scan()
		{
			skipWhitespace();
			while (position < expression.length())
			{
				scanToken(expression.charAt(position));
				skipWhitespace();
			}
		}

		private void scanToken(char current)
		{
			if (current == '"' || current == '\'')
				scanString(current);
			else if (isDigit(current))
				scanNumber();
			else if (isIdentifierStart(current))
				scanIdentifier();
			else if (current == '.')
				scanPropertyAccess();
			else if (BRACKETS_AND_SEPARATORS.indexOf(current) >= 0)
				scanBracketOrSeparator(current);
			else
				scanOperator(current);
		}

		private void scanString(char quote)
		{
			position++;
			while (position < expression.length() && expression.charAt(position) != quote)
			{
				if (expression.charAt(position) == '\\')
					position++;
				position++;
			}
			if (position >= expression.length())
				throw reject("unterminated string literal");
			position++;
			previous = TokenKind.OTHER;
		}

		private void scanNumber()
		{
			skipDigits();
			if (position + 1 < expression.length() && expression.charAt(position) == '.'
					&& isDigit(expression.charAt(position + 1)))
			{
				position++;
				skipDigits();
			}
			previous = TokenKind.OTHER;
		}

		private void scanIdentifier()
		{
			int start = position;
			while (position < expression.length() && isIdentifierPart(expression.charAt(position)))
				position++;
			String name = expression.substring(start, position);
			if (previous == TokenKind.DOT)
				assertAllowedProperty(name);
			else
				assertAllowedName(name);
			assertNotInvoked(name);
			previous = TokenKind.IDENTIFIER;
		}

		private void assertAllowedProperty(String name)
		{
			String lowerCaseName = name.toLowerCase(Locale.ROOT);
			if (lowerCaseName.contains("class") || FORBIDDEN_PROPERTIES.contains(lowerCaseName))
				throw reject("access to property '" + name + "' is not allowed");
		}

		private void assertAllowedName(String name)
		{
			if (!allowedVariables.contains(name) && !KEYWORDS.contains(name))
				throw reject("'" + name + "' is not an allowed variable");
		}

		private void assertNotInvoked(String name)
		{
			int next = position;
			while (next < expression.length() && Character.isWhitespace(expression.charAt(next)))
				next++;
			if (next < expression.length() && expression.charAt(next) == '(')
				throw reject("calling '" + name + "' is not allowed");
		}

		private void scanPropertyAccess()
		{
			if (previous != TokenKind.IDENTIFIER && previous != TokenKind.CLOSING_BRACKET)
				throw reject("property access is allowed only on variables");
			position++;
			if (position >= expression.length() || !isIdentifierStart(expression.charAt(position)))
				throw reject("property name expected after '.'");
			previous = TokenKind.DOT;
		}

		private void scanBracketOrSeparator(char current)
		{
			position++;
			previous = current == ')' || current == ']' ? TokenKind.CLOSING_BRACKET : TokenKind.OTHER;
		}

		private void scanOperator(char current)
		{
			if (position + 1 < expression.length()
					&& TWO_CHAR_OPERATORS.contains(expression.substring(position, position + 2)))
				position += 2;
			else if (SINGLE_CHAR_OPERATORS.indexOf(current) >= 0)
				position++;
			else
				throw reject("character '" + current + "' is not allowed");
			previous = TokenKind.OTHER;
		}

		private void skipWhitespace()
		{
			while (position < expression.length() && Character.isWhitespace(expression.charAt(position)))
				position++;
		}

		private void skipDigits()
		{
			while (position < expression.length() && isDigit(expression.charAt(position)))
				position++;
		}

		private ProjectFormValidationException reject(String reason)
		{
			return new ProjectFormValidationException(
					usage + " '" + expression + "' is not allowed in project forms: " + reason);
		}
	}

	private static boolean isDigit(char character)
	{
		return character >= '0' && character <= '9';
	}

	private static boolean isIdentifierStart(char character)
	{
		return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z') || character == '_';
	}

	private static boolean isIdentifierPart(char character)
	{
		return isIdentifierStart(character) || isDigit(character);
	}
}
