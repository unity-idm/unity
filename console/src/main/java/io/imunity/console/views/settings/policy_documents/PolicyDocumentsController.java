/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.settings.policy_documents;

import io.imunity.vaadin.elements.NotificationPresenter;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentCreateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentUpdateRequest;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
class PolicyDocumentsController
{
	private final MessageSource msg;
	private final PolicyDocumentManagement docMan;
	private final NotificationPresenter notificationPresenter;

	PolicyDocumentsController(MessageSource msg, PolicyDocumentManagement docMan, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.docMan = docMan;
		this.notificationPresenter = notificationPresenter;
	}

	public Collection<PolicyDocumentEntry> getPolicyDocuments()
	{
		try
		{
			return docMan.getPolicyDocuments().stream().map(PolicyDocumentEntry::new).toList();
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("PolicyDocumentsController.getAllError"), e.getMessage());
		}
		return List.of();
	}

	public void addPolicyDocument(PolicyDocumentEntry createRequest)
	{
		PolicyDocumentCreateRequest policyDocumentCreateRequest = new PolicyDocumentCreateRequest(createRequest.name, convert(createRequest.displayedName), createRequest.mandatory, createRequest.contentType, convert(createRequest.content));
		try
		{
			docMan.addPolicyDocument(policyDocumentCreateRequest);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("PolicyDocumentsController.addError", createRequest.name), e.getMessage());
		}
	}

	public PolicyDocumentEntry getPolicyDocument(Long id)
	{
		try
		{
			return new PolicyDocumentEntry(docMan.getPolicyDocument(id));
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("PolicyDocumentsController.getError", id), e.getMessage());
		}
		return null;
	}

	public void updatePolicyDocument(PolicyDocumentEntry updateRequest, boolean withRevsion)
	{
		PolicyDocumentUpdateRequest policyDocumentUpdateRequest = new PolicyDocumentUpdateRequest(updateRequest.id, updateRequest.name, convert(updateRequest.displayedName), updateRequest.mandatory, updateRequest.contentType, convert(updateRequest.content));
		try
		{
			if (withRevsion)
				docMan.updatePolicyDocumentWithRevision(policyDocumentUpdateRequest);
			else
				docMan.updatePolicyDocument(policyDocumentUpdateRequest);

		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("PolicyDocumentsController.updateError", updateRequest.id), e.getMessage());
		}

	}

	public void removePolicyDocument(Long id)
	{
		try
		{
			docMan.removePolicyDocument(id);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("PolicyDocumentsController.removeError", id), e.getMessage());
		}
	}

	private I18nString convert(Map<Locale, String> localizedValues)
	{
		I18nString i18nString = new I18nString();
		Map<String, String> map = localizedValues.entrySet().stream()
				.filter(entry -> !entry.getValue().isBlank())
				.collect(Collectors.toMap(entry -> entry.getKey().toString(), Map.Entry::getValue));
		i18nString.addAllValues(map);
		return i18nString;
	}
}
