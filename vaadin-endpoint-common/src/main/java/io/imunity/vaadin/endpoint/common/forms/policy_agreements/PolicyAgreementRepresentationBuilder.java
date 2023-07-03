/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.forms.policy_agreements;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.policy_document.PolicyDocumentContentType;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementConfigTextParser;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementConfigTextParser.DocPlaceholder;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement.POLICY_DOCUMENTS_PATH;

@Component
public class PolicyAgreementRepresentationBuilder
{
	private final PolicyDocumentManagement policyDocMan;
	private final SharedEndpointManagement sharedEndpointManagement;
	private final MessageSource msg;

	public PolicyAgreementRepresentationBuilder(MessageSource msg,
	                                            SharedEndpointManagement sharedEndpointManagement,
	                                            @Qualifier("insecure") PolicyDocumentManagement policyDocMan)
	{
		this.msg = msg;
		this.sharedEndpointManagement = sharedEndpointManagement;
		this.policyDocMan = policyDocMan;
	}

	public PolicyAgreementRepresentation getAgreementRepresentation(PolicyAgreementConfiguration agreementConfig)
	{
		List<PolicyDocumentWithRevision> resolvedDocs = resolvePolicyDoc(agreementConfig.documentsIdsToAccept);
		return new PolicyAgreementRepresentation(agreementConfig.documentsIdsToAccept,
				getAgreementTextRepresentation(agreementConfig.text, resolvedDocs),
				agreementConfig.presentationType,
				resolvedDocs.stream().anyMatch(d -> d.mandatory));
	}

	private List<PolicyDocumentWithRevision> resolvePolicyDoc(List<Long> docs)
	{
		try
		{
			return policyDocMan.getPolicyDocuments().stream().filter(d -> docs.contains(d.id))
					.collect(Collectors.toList());
		} catch (EngineException e)
		{
			throw new InternalException("Can not get policy documents");
		}

	}

	private String getAgreementTextRepresentation(I18nString agreementText, List<PolicyDocumentWithRevision> docs)
	{
		String text = agreementText.getValue(msg);
		if (text == null)
			return "";
		Map<Long, DocPlaceholder> allDocsPlaceholdersInConfigText = PolicyAgreementConfigTextParser
				.getAllDocsPlaceholdersInConfigText(text);
		String ret = text;
		for (DocPlaceholder dp : allDocsPlaceholdersInConfigText.values())
		{
			ret = ret.replaceAll(dp.toPatternString(),
					getLink(docs.stream().filter(d -> Objects.equals(d.id, dp.docId)).findFirst().orElse(null),
							dp.displayedText));
		}

		return ret;
	}

	private String getLink(PolicyDocumentWithRevision doc, String disp)
	{
		return "<a href=\"" + getPolicyDocumentPublicLink(doc) + "\" target=\"_blank\">" + disp + "</a>";
	}

	private String getPolicyDocumentPublicLink(PolicyDocumentWithRevision doc)
	{
		return doc != null
				? doc.contentType.equals(PolicyDocumentContentType.LINK)
				? doc.content.getValue(msg) : sharedEndpointManagement.getServletUrl(POLICY_DOCUMENTS_PATH) + doc.id
				: sharedEndpointManagement.getServletUrl(POLICY_DOCUMENTS_PATH) + "UNKNOWN";
	}
}
