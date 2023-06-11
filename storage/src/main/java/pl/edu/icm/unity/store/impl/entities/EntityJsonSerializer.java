/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.entities;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.base.identity.EntityInformation;
import pl.edu.icm.unity.store.rdbms.BaseBean;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;

/**
 * Serializes {@link StoredEntity} to/from DB form.
 * 
 * @author K. Benedyczak
 */
@Component
class EntityJsonSerializer implements RDBMSObjectSerializer<EntityInformation, BaseBean>
{
	@Autowired
	private ObjectMapper jsonMapper;
	
	@Override
	public BaseBean toDB(EntityInformation object)
	{
		try
		{
			BaseBean bean = new BaseBean(null,
					jsonMapper.writeValueAsBytes(EntityInformationBaseMapper.map(object)));
			bean.setId(object.getId());
			return bean;
		} catch (JsonProcessingException e)
		{
			throw new IllegalStateException("Error saving entity information to DB", e);

		}
	}

	@Override
	public EntityInformation fromDB(BaseBean bean)
	{
		DBEntityInformationBase entityInformationBase;
		try
		{
			entityInformationBase = jsonMapper.readValue(bean.getContents(), DBEntityInformationBase.class);
		} catch (IOException e)
		{
			throw new IllegalStateException("Error parsing entity information from DB", e);
		}

		return EntityInformationBaseMapper.map(entityInformationBase, bean.getId());
	}
}
