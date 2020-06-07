/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import pl.edu.icm.unity.store.types.StoredPolicyDocument;

/**
 * Policy document DAO
 * @author P.Piernik
 *
 */
public interface PolicyDocumentDAO extends BasicCRUDDAO<StoredPolicyDocument>
{
	String DAO_ID = "PolicyDocumentDAO";
	String NAME = "policyDocument";
}
