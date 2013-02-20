/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.json.JsonSerializer;
import pl.edu.icm.unity.db.json.SerializersRegistry;
import pl.edu.icm.unity.db.mapper.AttributesMapper;
import pl.edu.icm.unity.db.model.BaseBean;
import pl.edu.icm.unity.db.model.DBLimits;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.types.AttributeType;


/**
 * Attributes related DB operations.
 * @author K. Benedyczak
 */
@Component
public class DBAttributes
{
	private DBLimits limits;
	private JsonSerializer<AttributeType> serializer;
	
	@Autowired
	public DBAttributes(DB db, SerializersRegistry registry)
	{
		this.limits = db.getDBLimits();
		serializer = registry.getSerializer(AttributeType.class);
	}
	
	public void addAttributeType(AttributeType toAdd, SqlSession sqlMap)
	{
		if (toAdd.getName().length() > limits.getNameLimit())
			throw new IllegalGroupValueException("Attribute type name length must not exceed " + 
					limits.getNameLimit() + " characters");
		AttributesMapper mapper = sqlMap.getMapper(AttributesMapper.class);
		if (mapper.getAttributeType(toAdd.getName()) != null)
			throw new IllegalAttributeTypeException("The attribute type with name " + toAdd.getName() + 
					" already exists");
		
		BaseBean atb = new BaseBean(toAdd.getName(), serializer.toJson(toAdd));
		mapper.insertAttributeType(atb);
	}
	
	public List<AttributeType> getAttributeTypes(SqlSession sqlMap)
	{
		AttributesMapper mapper = sqlMap.getMapper(AttributesMapper.class);
		List<BaseBean> raw = mapper.getAttributeTypes();
		List<AttributeType> ret = new ArrayList<AttributeType>(raw.size());
		for (int i=0; i<raw.size(); i++)
		{
			BaseBean r = raw.get(i);
			AttributeType at = new AttributeType();
			at.setName(r.getName());
			serializer.fromJson(r.getContents(), at);
			ret.add(at);
		}
		return ret;
	}
}
