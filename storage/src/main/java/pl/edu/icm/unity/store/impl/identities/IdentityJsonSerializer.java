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
import pl.edu.icm.unity.store.types.StoredIdentity;
import pl.edu.icm.unity.types.basic.Identity;


/**
 * Identity JSON and DB serialization
 * @author K. Benedyczak
 */
@Component
public class IdentityJsonSerializer implements RDBMSObjectSerializer<StoredIdentity, IdentityBean>, 
		JsonSerializerForKryo<StoredIdentity>
{
	@Autowired
	private IdentityTypeDAO idTypeDAO;
	
	@Override
	public StoredIdentity fromJson(ObjectNode src)
	{
		return new StoredIdentity(new Identity(src));
	}

	@Override
	public ObjectNode toJson(StoredIdentity src)
	{
		return src.getIdentity().toJson();
	}

	@Override
	public IdentityBean toDB(StoredIdentity sobject)
	{
		Identity object = sobject.getIdentity();
		IdentityBean idB = new IdentityBean();
		idB.setEntityId(object.getEntityId());
		idB.setName(sobject.getName());
		long typeKey = idTypeDAO.getKeyForName(object.getTypeId());
		idB.setTypeId(typeKey);
		idB.setContents(JsonUtil.serialize2Bytes(object.toJsonBase()));
		return idB;
	}

	@Override
	public StoredIdentity fromDB(IdentityBean bean)
	{
		return new StoredIdentity(new Identity(bean.getTypeName(), bean.getEntityId(),
				JsonUtil.parse(bean.getContents())));
	}

	
	@Override
	public Class<? extends StoredIdentity> getClazz()
	{
		return StoredIdentity.class;
	}
}
