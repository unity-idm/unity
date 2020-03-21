/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.policyDocuments;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.PolicyDocumentDAO;
import pl.edu.icm.unity.store.hz.GenericBasicHzCRUD;
import pl.edu.icm.unity.store.types.StoredPolicyDocument;

/**
 * Hazelcast implementation of policy document store.
 * 
 * @author P.Piernik
 *
 */
@Repository(PolicyDocumentHzStore.STORE_ID)
public class PolicyDocumentHzStore extends GenericBasicHzCRUD<StoredPolicyDocument> implements PolicyDocumentDAO
{
	public static final String STORE_ID = DAO_ID + "hz";

	@Autowired
	public PolicyDocumentHzStore(PolicyDocumentRDBMSStore rdbmsDAO)
	{
		super(STORE_ID, NAME, PolicyDocumentRDBMSStore.BEAN, rdbmsDAO);
	}
}
