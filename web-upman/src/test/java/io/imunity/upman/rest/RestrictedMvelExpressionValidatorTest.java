/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class RestrictedMvelExpressionValidatorTest
{
	private final RestrictedMvelExpressionValidator validator = new RestrictedMvelExpressionValidator(
			Set.of("attr", "attrs", "idsByType", "idsByTypeObj", "groups", "status", "validCode", "agrs"));

	@ParameterizedTest
	@MethodSource("allowedExpressions")
	void shouldAcceptSideEffectFreeExpression(ExpressionCase expressionCase)
	{
		assertThatCode(() -> validator.validate(expressionCase.expression(), "Condition"))
				.doesNotThrowAnyException();
	}

	@ParameterizedTest
	@MethodSource("forbiddenExpressions")
	void shouldRejectExpressionAllowingCodeExecution(ExpressionCase expressionCase)
	{
		assertThatThrownBy(() -> validator.validate(expressionCase.expression(), "Condition"))
				.isInstanceOf(ProjectFormValidationException.class)
				.hasMessageContaining(expressionCase.expectedReason());
	}

	@Test
	void shouldAcceptMissingExpression()
	{
		assertThatCode(() -> validator.validate(null, "Condition")).doesNotThrowAnyException();
		assertThatCode(() -> validator.validate("  ", "Condition")).doesNotThrowAnyException();
	}

	private static Stream<ExpressionCase> allowedExpressions()
	{
		return Stream.of(
				allowed("true"),
				allowed("validCode == true"),
				allowed("attrs[\"email\"][0] != null"),
				allowed("idsByType[\"x500Name\"] != null"),
				allowed("agrs[0] == true"),
				allowed("idsByTypeObj[\"email\"][0].confirmed == true"),
				allowed("groups contains '/projects/A'"),
				allowed("!validCode && (status == 'submitted' || status == \"submittedAccepted\")"),
				allowed("attr['name'] + ' ' + attr['surname']"),
				allowed("attrs['age'][0] >= 18.5"),
				allowed("'literal with ; = new java.lang.Runtime() inside'"),
				allowed("'escaped \\' quote'"),
				allowed("attrs.empty == false"));
	}

	private static Stream<ExpressionCase> forbiddenExpressions()
	{
		return Stream.of(
				forbidden("java.lang.Runtime.getRuntime().exec('touch /tmp/x')", "'java' is not an allowed variable"),
				forbidden("Runtime.getRuntime()", "'Runtime' is not an allowed variable"),
				forbidden("new java.io.File('/tmp/x')", "'new' is not an allowed variable"),
				forbidden("attr.toString()", "calling 'toString' is not allowed"),
				forbidden("attrs ('x')", "calling 'attrs' is not allowed"),
				forbidden("attr.class", "access to property 'class'"),
				forbidden("attr.getClass", "access to property 'getClass'"),
				forbidden("attr.Class", "access to property 'Class'"),
				forbidden("attr.wait", "access to property 'wait'"),
				forbidden("'text'.bytes", "property access is allowed only on variables"),
				forbidden("1.class", "property access is allowed only on variables"),
				forbidden("attr. ", "property name expected"),
				forbidden("validCode = true", "character '='"),
				forbidden("validCode; true", "character ';'"),
				forbidden("validCode ? true : false", "character '?'"),
				forbidden("with (attr) { }", "'with' is not an allowed variable"),
				forbidden("def f() { true }", "'def' is not an allowed variable"),
				forbidden("import java.io.File", "'import' is not an allowed variable"),
				forbidden("attr instanceof java.lang.String", "'instanceof' is not an allowed variable"),
				forbidden("@{attr}", "character '@'"),
				forbidden("$x", "character '$'"),
				forbidden("attr['x'] | 1", "character '|'"),
				forbidden("unknownVariable == 1", "'unknownVariable' is not an allowed variable"),
				forbidden("'unterminated", "unterminated string literal"),
				forbidden("ąttr", "character 'ą'"));
	}

	private static ExpressionCase allowed(String expression)
	{
		return ExpressionCase.builder()
				.withExpression(expression)
				.build();
	}

	private static ExpressionCase forbidden(String expression, String expectedReason)
	{
		return ExpressionCase.builder()
				.withExpression(expression)
				.withExpectedReason(expectedReason)
				.build();
	}

	record ExpressionCase(String expression, String expectedReason)
	{
		static Builder builder()
		{
			return new Builder();
		}

		@Override
		public String toString()
		{
			return expression;
		}

		static class Builder
		{
			private String expression;
			private String expectedReason;

			Builder withExpression(String expression)
			{
				this.expression = expression;
				return this;
			}

			Builder withExpectedReason(String expectedReason)
			{
				this.expectedReason = expectedReason;
				return this;
			}

			ExpressionCase build()
			{
				return new ExpressionCase(expression, expectedReason);
			}
		}
	}
}
