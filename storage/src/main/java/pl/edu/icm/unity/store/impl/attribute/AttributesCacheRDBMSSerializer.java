/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypeRDBMSStore;
import pl.edu.icm.unity.store.impl.groups.GroupRDBMSStore;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.store.types.StoredAttribute;

/**
 * Serializes {@link StoredAttribute} to/from RDBMS {@link AttributesCacheBean}.
 */
@Component
public class AttributesCacheRDBMSSerializer implements RDBMSObjectSerializer<StoredAttribute, AttributesCacheBean>
{
	@Autowired
	private AttributeTypeRDBMSStore atDAO;
	@Autowired
	private GroupRDBMSStore groupDAO;
	@Autowired
	private ObjectMapper jsonMapper;

	@Override
	public AttributesCacheBean toDB(StoredAttribute object)
	{
		AttributesCacheBean bean = new AttributesCacheBean();
		bean.setEntityId(object.getEntityId());
		long groupId = groupDAO.getKeyForName(object.getAttribute().getGroupPath());
		bean.setGroupId(groupId);
		long typeId = atDAO.getKeyForName(object.getAttribute().getName());
		bean.setTypeId(typeId);
		try
		{
			bean.setValues(jsonMapper.writeValueAsBytes(AttributeExtBaseMapper.map(object.getAttribute())));
		} catch (JsonProcessingException e)
		{
			throw new IllegalStateException("Error saving cached attribute to DB", e);
		}
		return bean;
	}

	@Override
	public StoredAttribute fromDB(AttributesCacheBean bean)
	{
		AttributeExt attr;
		try
		{
			attr = AttributeExtBaseMapper.map(jsonMapper.readValue(bean.getValues(), DBAttributeExtBase.class),
					bean.getName(), bean.getValueSyntaxId(), bean.getGroup());
		} catch (IOException e)
		{
			throw new IllegalStateException("Error parsing cached attribute from DB", e);
		}
		return new StoredAttribute(attr, bean.getEntityId());
	}
}
