/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.processor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.jupiter.api.Test;

import eu.unicore.samly2.assertion.Assertion;

public class AssertionTimeConditionSetterTest
{
	@Test
	public void shouldSetOnlyNotBeforeCondition()
	{
		Assertion assertion = new Assertion();
		AssertionTimeConditionSetter.setDefaultNotBeforeCondition(assertion);

		assertThat(assertion.getNotBefore()).isEqualTo(assertion.getXMLBeanDoc()
				.getAssertion()
				.getIssueInstant()
				.getTime());
		assertThat(assertion.getNotOnOrAfter()).isNull();
	}

	@Test
	public void shouldPresaveNotOnOrAfterCondition()
	{
		Assertion assertion = new Assertion();
		Date notOnOrAfter = new Date();

		assertion.setTimeConditions(new Date(), notOnOrAfter);

		AssertionTimeConditionSetter.setDefaultNotBeforeCondition(assertion);

		assertThat(assertion.getNotBefore()).isEqualTo(assertion.getXMLBeanDoc()
				.getAssertion()
				.getIssueInstant()
				.getTime());
		assertThat(assertion.getNotOnOrAfter()).isEqualTo(notOnOrAfter);
	}
}
