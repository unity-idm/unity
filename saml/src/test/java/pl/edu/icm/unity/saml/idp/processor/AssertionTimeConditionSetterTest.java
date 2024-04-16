/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.processor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.assertion.Assertion;
import eu.unicore.samly2.elements.Subject;
import xmlbeans.org.oasis.saml2.assertion.SubjectConfirmationDataType;
import xmlbeans.org.oasis.saml2.assertion.SubjectConfirmationType;
import xmlbeans.org.oasis.saml2.assertion.SubjectType;

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
	public void shouldNotBeforeAlsoInSubjectConfirmationData()
	{
		Assertion assertion = new Assertion();

		assertion.setSubject(establishSubject());

		AssertionTimeConditionSetter.setDefaultNotBeforeCondition(assertion);

		assertThat(assertion.getNotBefore()).isEqualTo(assertion.getXMLBeanDoc()
				.getAssertion()
				.getIssueInstant()
				.getTime());
		assertThat(assertion.getXMLBeanDoc()
				.getAssertion()
				.getSubject()
				.getSubjectConfirmationArray()[0].getSubjectConfirmationData()
						.getNotBefore()
						.toInstant()).isEqualTo(assertion.getXMLBeanDoc()
								.getAssertion()
								.getIssueInstant()
								.getTime()
								.toInstant());
	}

	@Test
	public void shouldSetNotOnOrAfterInAssertion()
	{
		Assertion assertion = new Assertion();
		assertion.setSubject(establishSubject());
		AssertionTimeConditionSetter.setDefaultNotOnOrAfterInAssertion(assertion);
		assertThat(assertion.getNotOnOrAfter()
				.toInstant()).isEqualTo(
						assertion.getXMLBeanDoc()
								.getAssertion()
								.getSubject()
								.getSubjectConfirmationArray()[0].getSubjectConfirmationData()
										.getNotOnOrAfter()
										.toInstant());
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

	protected SubjectType establishSubject()
	{

		Subject authenticatedOne = new Subject("1", SAMLConstants.NFORMAT_PERSISTENT);
		SubjectType ret = authenticatedOne.getXBean();
		SubjectConfirmationType subConf = SubjectConfirmationType.Factory.newInstance();
		SubjectConfirmationDataType confData = subConf.addNewSubjectConfirmationData();
		Calendar validity = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		validity.setTimeInMillis(100);
		confData.setNotOnOrAfter(validity);
		ret.setSubjectConfirmationArray(new SubjectConfirmationType[]
		{ subConf });
		return ret;
	}
}
