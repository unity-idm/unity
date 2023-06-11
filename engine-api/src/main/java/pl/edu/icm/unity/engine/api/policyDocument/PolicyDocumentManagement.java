/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.policyDocument;

import java.util.Collection;

import pl.edu.icm.unity.base.exceptions.EngineException;

/**
 * Policy documents management API
 * 
 * @author P.Piernik
 *
 */
public interface PolicyDocumentManagement
{
	/**
	 * Adds new policy document
	 * 
	 * @param policyDocument
	 * @return created policy document id
	 * @throws EngineException
	 */
	long addPolicyDocument(PolicyDocumentCreateRequest policyDocument) throws EngineException;

	/**
	 * Updates policy document, skip revision update
	 * 
	 * @param policyDocument
	 * @throws EngineException
	 */
	void updatePolicyDocument(PolicyDocumentUpdateRequest policyDocument) throws EngineException;

	/**
	 * Updates policy document. Revision of the updated document is
	 * increment
	 * 
	 * @param policyDocument
	 * @throws EngineException
	 */
	void updatePolicyDocumentWithRevision(PolicyDocumentUpdateRequest policyDocument) throws EngineException;

	/**
	 * Removes policy document by given id
	 * 
	 * @param id
	 * @throws EngineException
	 */
	void removePolicyDocument(long id) throws EngineException;

	/**
	 * Gets all policy documents
	 * 
	 * @return
	 * @throws EngineException
	 */
	Collection<PolicyDocumentWithRevision> getPolicyDocuments() throws EngineException;

	/**
	 * Get single policy document by given id
	 * 
	 * @param id
	 * @return
	 * @throws EngineException
	 */
	PolicyDocumentWithRevision getPolicyDocument(long id) throws EngineException;

}
