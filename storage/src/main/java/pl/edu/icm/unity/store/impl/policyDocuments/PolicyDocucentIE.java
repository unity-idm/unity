/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.policyDocuments;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.api.PolicyDocumentDAO;
import pl.edu.icm.unity.store.export.AbstractIEBase;
import pl.edu.icm.unity.store.types.StoredPolicyDocument;

/**
 * Handles import/export of policy document.
 * 
 * @author P.Piernik
 *
 */
@Component
public class PolicyDocucentIE extends AbstractIEBase<StoredPolicyDocument>
{
	public static final String POLICY_DOCUMENTS_OBJECT_TYPE = "policyDocuments";

	private PolicyDocumentDAO dbIds;

	@Autowired
	public PolicyDocucentIE(PolicyDocumentDAO dbIds)
	{
		super(10, POLICY_DOCUMENTS_OBJECT_TYPE);
		this.dbIds = dbIds;
	}

	@Override
	protected List<StoredPolicyDocument> getAllToExport()
	{
		return dbIds.getAll();
	}

	@Override
	protected ObjectNode toJsonSingle(StoredPolicyDocument exportedObj)
	{
		return exportedObj.toJson();
	}

	@Override
	protected void createSingle(StoredPolicyDocument toCreate)
	{
		dbIds.createWithId(toCreate.getId(), toCreate);
	}

	@Override
	protected StoredPolicyDocument fromJsonSingle(ObjectNode src)
	{
		return new StoredPolicyDocument(src);
	}
}
