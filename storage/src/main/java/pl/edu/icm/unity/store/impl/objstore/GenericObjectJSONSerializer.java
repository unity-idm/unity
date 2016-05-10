/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.objstore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.hz.JsonSerializerForKryo;

/**
 * Noop serializer - in this case we use the in-DB bean as-is (is used in API) so no serialization is performed.
 * Actual serialziation is done in object-type specific way in higher level code.
 * @author K. Benedyczak
 */
@Component
public class GenericObjectJSONSerializer implements JsonSerializerForKryo<GenericObjectBean>
{
	@Autowired
	private ObjectMapper mapper;
	
	@Override
	public GenericObjectBean fromJson(ObjectNode src)
	{
		return mapper.convertValue(src, GenericObjectBean.class);
	}

	@Override
	public ObjectNode toJson(GenericObjectBean src)
	{
		return mapper.convertValue(src, ObjectNode.class);
	}

	@Override
	public Class<GenericObjectBean> getClazz()
	{
		return GenericObjectBean.class;
	}
}
