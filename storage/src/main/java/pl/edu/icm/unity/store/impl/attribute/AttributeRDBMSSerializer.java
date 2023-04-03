/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypeRDBMSStore;
import pl.edu.icm.unity.store.impl.groups.GroupRDBMSStore;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;


/**
 * Serializes {@link Attribute} to/from RDBMS {@link AttributeBean}.
 * @author K. Benedyczak
 */
@Component
public class AttributeRDBMSSerializer implements RDBMSObjectSerializer<StoredAttribute, AttributeBean>
{
	@Autowired
	private AttributeTypeRDBMSStore atDAO;
	@Autowired
	private GroupRDBMSStore groupDAO;
	
	@Override
	public AttributeBean toDB(StoredAttribute object)
	{
		AttributeBean bean = new AttributeBean();
		bean.setEntityId(object.getEntityId());
		long groupId = groupDAO.getKeyForName(object.getAttribute()
				.getGroupPath());
		bean.setGroupId(groupId);
		long typeId = atDAO.getKeyForName(object.getAttribute()
				.getName());
		bean.setTypeId(typeId);
		try
		{
			bean.setValues(Constants.MAPPER.writeValueAsBytes(AttributeExtBaseMapper.map(object.getAttribute())));
		} catch (JsonProcessingException e)
		{
			throw new IllegalStateException("Error saving attribute to DB", e);
		}
		return bean;
	}

	@Override
	public StoredAttribute fromDB(AttributeBean bean)
	{

		AttributeExt attr;

		try
		{
			attr = AttributeExtBaseMapper.map(Constants.MAPPER.readValue(bean.getValues(), DBAttributeExtBase.class),
					bean.getName(), bean.getValueSyntaxId(), bean.getGroup());
		} catch (IOException e)
		{
			throw new IllegalStateException("Error parsing attribute from DB", e);
		}

		return new StoredAttribute(attr, bean.getEntityId());
	}
}
