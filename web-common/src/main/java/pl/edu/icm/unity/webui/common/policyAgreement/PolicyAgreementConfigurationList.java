/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.common.policyAgreement;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.types.policyAgreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.webui.common.ListOfDnDCollapsableElements;

public class PolicyAgreementConfigurationList extends ListOfDnDCollapsableElements<PolicyAgreementConfiguration>
{
	public PolicyAgreementConfigurationList(UnityMessageSource msg,
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
		private final UnityMessageSource msg;
		private final PolicyDocumentManagement policyDocMan;
		
		PolicyAgreementConfigurationListFactory(UnityMessageSource msg, PolicyDocumentManagement policyDocMan)
		{
			super();
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