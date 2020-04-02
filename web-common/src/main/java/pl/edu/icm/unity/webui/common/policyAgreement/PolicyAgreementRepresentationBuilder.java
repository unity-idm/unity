/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.policyAgreement;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.policyDocument.PolicyDocumentContentType;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementConfigTextParser;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementConfigTextParser.DocPlaceholder;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.engine.api.wellknown.PublicWellKnownURLServletProvider;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.I18nString;

/**
 * Builds single policy agreement configuration representation.
 * 
 * @author P.Piernik
 *
 */
@Component
public class PolicyAgreementRepresentationBuilder
{
	private PolicyDocumentManagement policyDocMan;
	private SharedEndpointManagement sharedEndpointManagement;
	private UnityMessageSource msg;

	public PolicyAgreementRepresentationBuilder(UnityMessageSource msg,
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
				resolvedDocs.stream().filter(d -> d.mandatory).findFirst().isPresent());
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
		Map<Long, DocPlaceholder> allDocsPlaceholdersInConfigText = PolicyAgreementConfigTextParser
				.getAllDocsPlaceholdersInConfigText(text);
		String ret = new String(text);
		for (DocPlaceholder dp : allDocsPlaceholdersInConfigText.values())
		{
			ret = ret.replaceAll(dp.toPatternString(),
					getLink(docs.stream().filter(d -> d.id == dp.docId).findFirst().orElse(null),
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
				? doc.contentType.equals(PolicyDocumentContentType.LINK) ? doc.content.getValue(msg)
						: sharedEndpointManagement.getServletUrl(
								PublicWellKnownURLServletProvider.SERVLET_PATH) + "?"
								+ PublicPolicyDocumentView.POLICY_DOC_PARAM + "="
								+ doc.id + "#!"
								+ PublicPolicyDocumentView.POLICY_DOC_VIEW
				: sharedEndpointManagement.getServletUrl(PublicWellKnownURLServletProvider.SERVLET_PATH)
						+ "?" + PublicPolicyDocumentView.POLICY_DOC_PARAM + "=UNKNOWN" + "#!"
						+ PublicPolicyDocumentView.POLICY_DOC_VIEW;
	}
}
