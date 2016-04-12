/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.attribute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.generic.DependencyNotificationManager;
import pl.edu.icm.unity.store.rdbms.DBLimit;
import pl.edu.icm.unity.store.rdbms.mapper.AttributesMapper;
import pl.edu.icm.unity.store.rdbms.model.AttributeTypeBean;
import pl.edu.icm.unity.store.tx.TransactionTL;
import pl.edu.icm.unity.types.basic.AttributeType;


/**
 * RDBMS storage of {@link AttributeType}
 * @author K. Benedyczak
 */
@Repository
public class AttributeTypeRDBMSStore implements AttributeTypeDAO
{
	public static final String ATTRIBUTE_TYPES_NOTIFICATION_ID = "attributeTypes";

	@Autowired
	private AttributeTypeJsonSerializer atSerializer;
	@Autowired
	private DependencyNotificationManager notificationsManager;
	@Autowired
	private DBLimit limits;
	
	@Override
	public void create(AttributeType toAdd)
	{
		SqlSession sqlMap = TransactionTL.getSql();
		limits.checkNameLimit(toAdd.getName());
		AttributesMapper mapper = sqlMap.getMapper(AttributesMapper.class);
		if (mapper.getAttributeType(toAdd.getName()) != null)
			throw new IllegalArgumentException("The attribute type with name " + toAdd.getName() + 
					" already exists");
		
		AttributeTypeBean atb = atSerializer.toDB(toAdd);
		notificationsManager.firePreAddEvent(ATTRIBUTE_TYPES_NOTIFICATION_ID, toAdd, sqlMap);
		mapper.insertAttributeType(atb);
	}

	@Override
	public AttributeType get(String id)
	{
		AttributesMapper mapper = TransactionTL.getSql().getMapper(AttributesMapper.class);
		AttributeTypeBean atBean = mapper.getAttributeType(id);
		if (atBean == null)
			throw new IllegalArgumentException("The attribute type with name " + id + 
					" does not exist");
		return atSerializer.fromDB(atBean);
	}

	@Override
	public void delete(String id)
	{
		AttributesMapper mapper = TransactionTL.getSql().getMapper(AttributesMapper.class);
		AttributeType removed = get(id);
		notificationsManager.firePreRemoveEvent(ATTRIBUTE_TYPES_NOTIFICATION_ID, removed, 
				TransactionTL.getSql());
		mapper.deleteAttributeType(id);
	}

	@Override
	public void update(AttributeType toUpdate)
	{
		limits.checkNameLimit(toUpdate.getName());
		AttributeType old = get(toUpdate.getName());
		notificationsManager.firePreUpdateEvent(ATTRIBUTE_TYPES_NOTIFICATION_ID, old, toUpdate, 
				TransactionTL.getSql());

		AttributesMapper mapper = TransactionTL.getSql().getMapper(AttributesMapper.class);
		AttributeTypeBean updatedB = atSerializer.toDB(toUpdate);
		mapper.updateAttributeType(updatedB);
	}

	@Override
	public Map<String, AttributeType> getAsMap()
	{
		AttributesMapper mapper = TransactionTL.getSql().getMapper(AttributesMapper.class);
		List<AttributeTypeBean> raw = mapper.getAttributeTypes();
		Map<String, AttributeType> ret = new HashMap<String, AttributeType>(raw.size());
		for (AttributeTypeBean r: raw)
			ret.put(r.getName(), atSerializer.fromDB(r));
		return ret;
	}

	@Override
	public boolean exists(String id)
	{
		AttributesMapper mapper = TransactionTL.getSql().getMapper(AttributesMapper.class);
		AttributeTypeBean atBean = mapper.getAttributeType(id);
		return atBean != null;
	}
}
