/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.policyDocuments;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.rdbms.BaseBean;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.store.types.StoredPolicyDocument;

/**
 * Serializes {@link StoredPolicyDocument} to/from DB form.
 * @author P.Piernik
 *
 */
@Component
class PolicyDocumentJsonSerializer implements RDBMSObjectSerializer<StoredPolicyDocument, BaseBean>
{
	@Override
	public BaseBean toDB(StoredPolicyDocument object)
	{
		BaseBean ret = new BaseBean(object.getName(), JsonUtil.serialize2Bytes(object.toJsonBase()));
		ret.setId(object.getId());
		return ret;
	}

	@Override
	public StoredPolicyDocument fromDB(BaseBean bean)
	{
		StoredPolicyDocument ret = new StoredPolicyDocument(bean.getId(), bean.getName());
		ret.fromJsonBase(JsonUtil.parse(bean.getContents()));
		return ret;
	}
}
