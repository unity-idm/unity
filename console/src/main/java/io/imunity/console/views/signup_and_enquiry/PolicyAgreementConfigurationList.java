/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.console.views.signup_and_enquiry;

import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

public class PolicyAgreementConfigurationList extends CollapsableGrid<PolicyAgreementConfiguration>
{
	public PolicyAgreementConfigurationList(MessageSource msg,
			Supplier<CollapsableGrid.Editor<PolicyAgreementConfiguration>> editorProvider)
	{
		super(msg, editorProvider, () -> new PolicyAgreementConfiguration(Collections.emptyList(),
				PolicyAgreementPresentationType.CHECKBOX_SELECTED, new I18nString()),  msg.getMessage("PolicyAgreementList.items"));
	}

	@Component
	public static class PolicyAgreementConfigurationListFactory
	{
		private final MessageSource msg;
		private final PolicyDocumentManagement policyDocMan;
		
		PolicyAgreementConfigurationListFactory(MessageSource msg, PolicyDocumentManagement policyDocMan)
		{
			this.msg = msg;
			this.policyDocMan = policyDocMan;
		}
		
		public PolicyAgreementConfigurationList getInstance() throws EngineException
		{	
			Collection<PolicyDocumentWithRevision> policyDocuments = policyDocMan.getPolicyDocuments();
			return new PolicyAgreementConfigurationList(msg,
					() -> new PolicyAgreementConfigurationEditor(msg, policyDocuments));
		}
	}
}