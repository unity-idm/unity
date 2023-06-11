/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.common.policyAgreement;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.policyAgreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.webui.common.ListOfDnDCollapsableElements;

public class PolicyAgreementConfigurationList extends ListOfDnDCollapsableElements<PolicyAgreementConfiguration>
{
	public PolicyAgreementConfigurationList(MessageSource msg,
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