/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identities;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.rdbms.GenericNamedRDBMSCRUD;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;
import pl.edu.icm.unity.types.basic.Identity;


/**
 * RDBMS storage of {@link Identity}
 * @author K. Benedyczak
 */
@Repository(IdentityRDBMSStore.BEAN)
public class IdentityRDBMSStore extends GenericNamedRDBMSCRUD<Identity, IdentityBean> implements IdentityDAO
{
	public static final String BEAN = DAO_ID + "rdbms";

	@Autowired
	public IdentityRDBMSStore(IdentityJsonSerializer jsonSerializer)
	{
		super(IdentitiesMapper.class, jsonSerializer, NAME);
	}

	@Override
	public List<Identity> getByEntity(long entityId)
	{
		IdentitiesMapper mapper = SQLTransactionTL.getSql().getMapper(IdentitiesMapper.class);
		List<IdentityBean> allInDB = mapper.getByEntity(entityId);
		List<Identity> ret = new ArrayList<>(allInDB.size());
		for (IdentityBean bean: allInDB)
			ret.add(jsonSerializer.fromDB(bean));
		return ret;
	}
	
	@Override
	protected void preUpdateCheck(IdentityBean oldBean, Identity updated)
	{
		Identity old = jsonSerializer.fromDB(oldBean);
		if (!old.getTypeId().equals(updated.getTypeId()))
			throw new IllegalArgumentException("Can not change identity type from " + 
					old.getTypeId() + " to " + updated.getTypeId());
		if (!old.getComparableValue().equals(updated.getComparableValue()))
			throw new IllegalArgumentException("Can not change identity value from " + 
					old.getComparableValue() + " to " + updated.getValue());
		
	}
}
