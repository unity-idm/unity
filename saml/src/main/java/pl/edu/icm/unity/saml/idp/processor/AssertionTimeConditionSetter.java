/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.processor;

import java.util.Calendar;
import java.util.Optional;
import java.util.TimeZone;

import eu.unicore.samly2.assertion.Assertion;
import xmlbeans.org.oasis.saml2.assertion.ConditionsType;
import xmlbeans.org.oasis.saml2.assertion.SubjectConfirmationDataType;
import xmlbeans.org.oasis.saml2.assertion.SubjectConfirmationType;
import xmlbeans.org.oasis.saml2.assertion.SubjectType;

public class AssertionTimeConditionSetter
{
	static void setDefaultNotBeforeCondition(Assertion assertion)
	{
		ConditionsType conditions = getOrCreateConditions(assertion);
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		c.setTime(assertion.getXMLBeanDoc().getAssertion().getIssueInstant().getTime());
		conditions.setNotBefore(c);		
		Optional<SubjectConfirmationDataType> subcjectConfirmationData = getSubcjectConfirmationData(assertion);
		if (subcjectConfirmationData.isEmpty())
			return;
		subcjectConfirmationData.get().setNotBefore(c);
	}
	
	public static void setDefaultNotOnOrAfterInAssertion(Assertion assertion)
	{
		Optional<SubjectConfirmationDataType> subcjectConfirmationData = getSubcjectConfirmationData(assertion);
		if (subcjectConfirmationData.isEmpty())
			return;
		if (subcjectConfirmationData.get().getNotOnOrAfter() != null)
		{
			getOrCreateConditions(assertion).setNotOnOrAfter(subcjectConfirmationData.get().getNotOnOrAfter() );
		}
	}
	
	static ConditionsType getOrCreateConditions(Assertion assertion)
	{
		ConditionsType conditions = assertion.getXMLBeanDoc().getAssertion().getConditions();
		if (conditions == null)
			return assertion.getXMLBeanDoc().getAssertion().addNewConditions();
		return conditions;
	}

	
	static Optional<SubjectConfirmationDataType> getSubcjectConfirmationData(Assertion assertion)
	{
		SubjectType subject = assertion.getXMLBeanDoc().getAssertion().getSubject();
		if (subject == null)
			return Optional.empty();
		SubjectConfirmationType[] subjectConfirmationArray = subject.getSubjectConfirmationArray();
		if (subjectConfirmationArray == null || subjectConfirmationArray.length == 0)
		{
			return Optional.empty();
		}
		return Optional.ofNullable(subjectConfirmationArray[0].getSubjectConfirmationData());
	}
	
}
