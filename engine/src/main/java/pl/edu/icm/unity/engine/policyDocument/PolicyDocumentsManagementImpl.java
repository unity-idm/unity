/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.policyDocument;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.capacityLimit.CapacityLimitName;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentCreateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentUpdateRequest;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.engine.capacityLimits.InternalCapacityLimitVerificator;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.PolicyDocumentDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.types.StoredPolicyDocument;

/**
 * Implements policy documents operations.
 * 
 * @author P.Piernik
 *
 */
@Component
@Primary
@InvocationEventProducer
public class PolicyDocumentsManagementImpl implements PolicyDocumentManagement
{
	private PolicyDocumentDAO dao;
	private InternalAuthorizationManager authz;
	private InternalCapacityLimitVerificator capacityLimitVerificator;

	@Autowired
	PolicyDocumentsManagementImpl(InternalAuthorizationManager authz, PolicyDocumentDAO dao,
			InternalCapacityLimitVerificator capacityLimitVerificator)
	{
		this.dao = dao;
		this.authz = authz;
		this.capacityLimitVerificator = capacityLimitVerificator;
	}

	@Override
	@Transactional
	public long addPolicyDocument(PolicyDocumentCreateRequest policyDocument) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.policyDocumentsModify);
		capacityLimitVerificator.assertInSystemLimitForSingleAdd(CapacityLimitName.PolicyDocumentsCount,
				() -> dao.getCount());
		return dao.create(toStoredPolicyDocument(policyDocument));

	}

	@Override
	@Transactional
	public void updatePolicyDocument(PolicyDocumentUpdateRequest policyDocument) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.policyDocumentsModify);
		StoredPolicyDocument org = dao.getByKey(policyDocument.id);
		StoredPolicyDocument storedPolicyDocument = toStoredPolicyDocument(policyDocument, org.getRevision());
		storedPolicyDocument.setId(org.getId());
		dao.updateByKey(policyDocument.id, storedPolicyDocument);

	}

	@Override
	@Transactional
	public void updatePolicyDocumentWithRevision(PolicyDocumentUpdateRequest policyDocument) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.policyDocumentsModify);
		StoredPolicyDocument org = dao.getByKey(policyDocument.id);
		StoredPolicyDocument storedPolicyDocument = toStoredPolicyDocument(policyDocument,
				org.getRevision() + 1);
		storedPolicyDocument.setId(org.getId());
		dao.updateByKey(policyDocument.id, storedPolicyDocument);

	}

	@Override
	@Transactional
	public void removePolicyDocument(long id) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.policyDocumentsModify);
		dao.deleteByKey(id);
	}

	@Override
	@Transactional
	public Collection<PolicyDocumentWithRevision> getPolicyDocuments() throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.policyDocumentsRead);
		return dao.getAll().stream().map(d -> toPolicyDocumentWithRevision(d)).collect(Collectors.toList());
	}

	@Override
	@Transactional
	public PolicyDocumentWithRevision getPolicyDocument(long id) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.policyDocumentsRead);
		return toPolicyDocumentWithRevision(dao.getByKey(id));
	}

	private StoredPolicyDocument toStoredPolicyDocument(PolicyDocumentUpdateRequest pd, int revision)
	{
		StoredPolicyDocument doc = toStoredPolicyDocument(pd);
		doc.setId(pd.id);
		doc.setRevision(revision);
		return doc;
	}

	private StoredPolicyDocument toStoredPolicyDocument(PolicyDocumentCreateRequest pd)
	{
		StoredPolicyDocument doc = new StoredPolicyDocument();
		doc.setName(pd.name);
		doc.setContent(pd.content);
		doc.setDisplayedName(pd.displayedName);
		doc.setMandatory(pd.mandatory);
		doc.setContentType(pd.contentType);
		return doc;
	}

	private PolicyDocumentWithRevision toPolicyDocumentWithRevision(StoredPolicyDocument sd)
	{
		return new PolicyDocumentWithRevision(sd.getId(), sd.getName(), sd.getDisplayedName(), sd.isMandatory(),
				sd.getContentType(), sd.getContent(), sd.getRevision());
	}
}
