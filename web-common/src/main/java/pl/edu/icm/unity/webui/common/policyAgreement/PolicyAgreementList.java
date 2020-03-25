/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.common.policyAgreement;

import java.util.Collections;
import java.util.function.Supplier;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.common.ListOfDnDCollapsableElements;

public class PolicyAgreementList extends ListOfDnDCollapsableElements<PolicyAgreementConfiguration>
{
	public PolicyAgreementList(UnityMessageSource msg,
			Supplier<Editor<PolicyAgreementConfiguration>> editorProvider)
	{
		super(msg, editorProvider, msg.getMessage("PolicyAgreementList.items"));
	}

	@Override
	protected PolicyAgreementConfiguration makeNewInstance()
	{
		return new PolicyAgreementConfiguration(Collections.emptyList(),
				PolicyAgreementPresentationType.CHECKBOX_SELECTED, new I18nString());
	}
}