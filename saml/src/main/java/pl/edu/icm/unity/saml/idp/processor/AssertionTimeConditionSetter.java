/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.processor;

import java.util.Calendar;
import java.util.TimeZone;

import eu.unicore.samly2.assertion.Assertion;
import xmlbeans.org.oasis.saml2.assertion.ConditionsType;

public class AssertionTimeConditionSetter
{
	static void setDefaultNotBeforeCondition(Assertion assertion)
	{
		ConditionsType conditions = assertion.getXMLBeanDoc().getAssertion().getConditions();
		if (conditions == null)
			conditions = assertion.getXMLBeanDoc().getAssertion().addNewConditions();
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		c.setTime(assertion.getXMLBeanDoc().getAssertion().getIssueInstant().getTime());
		conditions.setNotBefore(c);
	}
}
