/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identities;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.impl.identitytype.IdentityTypeRDBMSStore;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.store.types.StoredIdentity;
import pl.edu.icm.unity.types.basic.Identity;


/**
 * Identity JSON and DB serialization
 * @author K. Benedyczak
 */
@Component
class IdentityJsonSerializer implements RDBMSObjectSerializer<StoredIdentity, IdentityBean>
{
	private final IdentityTypeRDBMSStore idTypeDAO;
	
	IdentityJsonSerializer(IdentityTypeRDBMSStore idTypeDAO)
	{
		this.idTypeDAO = idTypeDAO;
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
}
