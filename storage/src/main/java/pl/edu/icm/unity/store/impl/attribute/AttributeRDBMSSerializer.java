/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.StoredAttribute;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;


/**
 * Serializes {@link Attribute} to/from RDBMS {@link AttributeBean}.
 * @author K. Benedyczak
 */
@Component
public class AttributeRDBMSSerializer implements RDBMSObjectSerializer<StoredAttribute, AttributeBean>
{
	@Autowired
	private AttributeTypeDAO atDAO;
	@Autowired
	private GroupDAO groupDAO;
	@Autowired
	private AttributeJsonSerializer jsonSerializer;
	
	@Override
	public AttributeBean toDB(StoredAttribute object)
	{
		AttributeBean bean = new AttributeBean();
		bean.setEntityId(object.getEntityId());
		long groupId = groupDAO.getKeyForName(object.getAttribute().getGroupPath());
		bean.setGroupId(groupId);
		long typeId = atDAO.getKeyForName(object.getAttribute().getName());
		bean.setTypeId(typeId);
		bean.setValues(JsonUtil.serialize2Bytes(jsonSerializer.toJsonBase(object.getAttribute())));
		return bean;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public StoredAttribute fromDB(AttributeBean bean)
	{
		AttributeExt attr = new AttributeExt();
		attr.setName(bean.getName());
		attr.setGroupPath(bean.getGroup());
		AttributeType type = atDAO.get(bean.getValueSyntaxId());
		attr.setAttributeSyntax(type.getValueType());
		attr.setDirect(true);
		jsonSerializer.fromJsonBase(JsonUtil.parse(bean.getValues()), attr);
		return new StoredAttribute(attr, bean.getEntityId());
	}
}
