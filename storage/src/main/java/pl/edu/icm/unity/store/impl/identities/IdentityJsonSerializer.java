/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.hz.JsonSerializerForKryo;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.types.basic.Identity;


/**
 * Identity JSON and DB serialization
 * @author K. Benedyczak
 */
@Component
public class IdentityJsonSerializer implements RDBMSObjectSerializer<Identity, IdentityBean>, 
		JsonSerializerForKryo<Identity>
{
	@Autowired
	private IdentityTypeDAO idTypeDAO;
	
	@Override
	public Identity fromJson(ObjectNode src)
	{
		return new Identity(src);
	}

	@Override
	public ObjectNode toJson(Identity src)
	{
		return src.toJson();
	}

	@Override
	public IdentityBean toDB(Identity object)
	{
		IdentityBean idB = new IdentityBean();
		idB.setEntityId(object.getEntityId());
		idB.setName(object.getComparableValue());
		long typeKey = idTypeDAO.getKeyForName(object.getTypeId());
		idB.setTypeId(typeKey);
		idB.setContents(JsonUtil.serialize2Bytes(object.toJsonBase()));
		return idB;
	}

	@Override
	public Identity fromDB(IdentityBean bean)
	{
		return new Identity(bean.getTypeName(), bean.getEntityId(),
				bean.getName(), JsonUtil.parse(bean.getContents()));
	}

	
	@Override
	public Class<? extends Identity> getClazz()
	{
		return Identity.class;
	}
}
