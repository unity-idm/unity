/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identitytype;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.hz.JsonSerializerForKryo;
import pl.edu.icm.unity.store.rdbms.BaseBean;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Handles serialization of {@link IdentityType} metadata. The metadata
 * is common for all identity types.
 * @author K. Benedyczak
 */
@Component
public class IdentityTypeJsonSerializer implements RDBMSObjectSerializer<IdentityType, BaseBean>,
	JsonSerializerForKryo<IdentityType>
{
	@Override
	public IdentityType fromDB(BaseBean raw)
	{
		IdentityType it = new IdentityType(raw.getName());
		it.fromJsonBase(JsonUtil.parse(raw.getContents()));
		return it;
	}
	
	@Override
	public BaseBean toDB(IdentityType idType)
	{
		BaseBean toAdd = new BaseBean();

		toAdd.setName(idType.getName());
		toAdd.setContents(JsonUtil.serialize2Bytes(idType.toJsonBase()));
		return toAdd;
	}

	@Override
	public IdentityType fromJson(ObjectNode main)
	{
		return new IdentityType(main);
	}
	
	@Override
	public ObjectNode toJson(IdentityType src)
	{
		return src.toJson();
	}

	@Override
	public Class<IdentityType> getClazz()
	{
		return IdentityType.class;
	}
}



