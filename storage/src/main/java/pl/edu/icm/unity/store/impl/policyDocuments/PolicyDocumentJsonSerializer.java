/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.policyDocuments;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	@Autowired
	private ObjectMapper jsonMapper;
	
	@Override
	public BaseBean toDB(StoredPolicyDocument object)
	{
		BaseBean toAdd = new BaseBean();
		toAdd.setName(object.getName());
		toAdd.setId(object.getId());
		try
		{		
			toAdd.setContents(jsonMapper.writeValueAsBytes(PolicyDocumentBaseMapper.map(object)));
		
		} catch (JsonProcessingException e)
		{
			throw new IllegalStateException("Error saving policy document to DB", e);
		}
		return toAdd;
	}

	@Override
	public StoredPolicyDocument fromDB(BaseBean bean)
	{
		try
		{
			return
					PolicyDocumentBaseMapper.map(jsonMapper.readValue(bean.getContents(), DBPolicyDocumentBase.class), bean.getId(), bean.getName());
		} catch (IOException e)
		{
			throw new IllegalStateException("Error parsing policy document from DB", e);
		}
		
	}
}
