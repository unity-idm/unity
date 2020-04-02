/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.policyAgreement;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.api.attributes.SystemAttributesProvider;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeType;

@Component
public class PolicyAgreementStateAttributeProvider implements SystemAttributesProvider
{
	private UnityMessageSource msg;

	@Autowired
	PolicyAgreementStateAttributeProvider(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	private AttributeType getAttributeType()
	{

		StringAttributeSyntax syntax = new StringAttributeSyntax();
		AttributeType policyAgreementAt = new AttributeType(PolicyAgreementManagementImpl.POLICY_AGREEMENT_STATE, StringAttributeSyntax.ID,
				msg);
		policyAgreementAt.setFlags(AttributeType.TYPE_IMMUTABLE_FLAG);
		policyAgreementAt.setMinElements(1);
		policyAgreementAt.setMaxElements(99);
		policyAgreementAt.setUniqueValues(false);
		policyAgreementAt.setValueSyntaxConfiguration(syntax.getSerializedConfiguration());
		return policyAgreementAt;
	}

	@Override
	public List<AttributeType> getSystemAttributes()
	{
		return Lists.newArrayList(getAttributeType());
	}

	@Override
	public boolean requiresUpdate(AttributeType at)
	{
		return false;
	}
}
