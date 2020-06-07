/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.policyDocuments;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.PolicyDocumentDAO;
import pl.edu.icm.unity.store.rdbms.BaseBean;
import pl.edu.icm.unity.store.rdbms.GenericRDBMSCRUD;
import pl.edu.icm.unity.store.types.StoredPolicyDocument;

/**
 * RDBMS storage of {@link StoredPolicyDocument}
 * @author P.Piernik
 *
 */
@Repository(PolicyDocumentRDBMSStore.BEAN)
public class PolicyDocumentRDBMSStore extends GenericRDBMSCRUD<StoredPolicyDocument, BaseBean> implements PolicyDocumentDAO
{
	public static final String BEAN = DAO_ID + "rdbms";

	@Autowired
	public PolicyDocumentRDBMSStore(PolicyDocumentJsonSerializer jsonSerializer)
	{
		super(PolicyDocumentsMapper.class, jsonSerializer, NAME);
	}
	
	@Override
	public long create(StoredPolicyDocument obj)
	{
		long ret = super.create(obj);
		obj.setId(ret);
		return ret;
	}

}
