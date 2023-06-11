/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.settings.policyDocuments;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentCreateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentUpdateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

@Component
class PolicyDocumentsController
{
	private MessageSource msg;
	private PolicyDocumentManagement docMan;

	PolicyDocumentsController(MessageSource msg, PolicyDocumentManagement docMan)
	{
		this.msg = msg;
		this.docMan = docMan;
	}

	public Collection<PolicyDocumentEntry> getPolicyDocuments() throws ControllerException
	{
		try
		{
			return docMan.getPolicyDocuments().stream().map(d -> new PolicyDocumentEntry(d))
					.collect(Collectors.toList());
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("PolicyDocumentsController.getAllError"), e);
		}
	}

	public void addPolicyDocument(PolicyDocumentCreateRequest doc) throws ControllerException
	{
		try
		{
			docMan.addPolicyDocument(doc);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("PolicyDocumentsController.addError", doc.name),
					e);
		}
	}

	public PolicyDocumentWithRevision getPolicyDocument(Long id) throws ControllerException
	{
		try
		{
			return docMan.getPolicyDocument(id);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("PolicyDocumentsController.getError", id), e);
		}
	}

	public void updatePolicyDocument(PolicyDocumentUpdateRequest updateRequest, boolean withRevsion)
			throws ControllerException
	{
		try
		{
			if (withRevsion)
			{
				docMan.updatePolicyDocumentWithRevision(updateRequest);

			} else
			{
				docMan.updatePolicyDocument(updateRequest);

			}

		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("PolicyDocumentsController.updateError", updateRequest.id), e);
		}

	}

	public void removePolicyDocument(PolicyDocumentWithRevision doc) throws ControllerException
	{
		try
		{
			docMan.removePolicyDocument(doc.id);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("PolicyDocumentsController.removeError", doc.id),
					e);
		}

	}

	public PolicyDocumentEditor getEditor(PolicyDocumentWithRevision doc) throws ControllerException
	{
		try
		{
			return new PolicyDocumentEditor(msg, doc, docMan.getPolicyDocuments().stream().map(d -> d.name)
					.filter(d -> doc == null || !d.equals(doc.name)).collect(Collectors.toSet()));
		} catch (EngineException e)
		{
			throw new ControllerException(
					msg.getMessage("PolicyDocumentsController.createEditorError", doc.id), e);
		}
	}
}
