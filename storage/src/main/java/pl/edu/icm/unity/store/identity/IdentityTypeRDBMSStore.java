/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.identity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.rdbms.mapper.IdentitiesMapper;
import pl.edu.icm.unity.store.rdbms.model.BaseBean;
import pl.edu.icm.unity.store.tx.TransactionTL;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Implementation of {@link IdentityTypeDAO}.
 * 
 * @author K. Benedyczak
 */
@Repository
public class IdentityTypeRDBMSStore implements IdentityTypeDAO
{
	@Autowired
	private IdentityTypeJsonSerializer idTypeSerializer;
	
	@Override
	public Map<String, IdentityType> getAsMap()
	{
		IdentitiesMapper mapper = TransactionTL.getSql().getMapper(IdentitiesMapper.class);
		List<BaseBean> identityTypeState = mapper.getIdentityTypes();
		Map<String, IdentityType> ret = new HashMap<>(identityTypeState.size());
		for (BaseBean state: identityTypeState)
			ret.put(state.getName(), idTypeSerializer.fromDB(state));
		return ret;
	}

	@Override
	public void create(IdentityType idType)
	{
		IdentitiesMapper mapper = TransactionTL.getSql().getMapper(IdentitiesMapper.class);
		BaseBean toAdd = idTypeSerializer.toDB(idType);
		mapper.insertIdentityType(toAdd);
	}

	@Override
	public void update(IdentityType idType)
	{
		IdentitiesMapper mapper = TransactionTL.getSql().getMapper(IdentitiesMapper.class);
		BaseBean toUpdate = idTypeSerializer.toDB(idType);
		mapper.updateIdentityType(toUpdate);
	}

	@Override
	public void delete(String idType)
	{
		IdentitiesMapper mapper = TransactionTL.getSql().getMapper(IdentitiesMapper.class);
		mapper.deleteIdentityType(idType);
	}

	@Override
	public IdentityType get(String idType)
	{
		IdentitiesMapper mapper = TransactionTL.getSql().getMapper(IdentitiesMapper.class);
		BaseBean identityTypeState = mapper.getIdentityTypeByName(idType);
		if (identityTypeState == null)
			throw new IllegalArgumentException("The identity type with name " + idType + 
					" does not exist");
		return idTypeSerializer.fromDB(identityTypeState);
	}

	@Override
	public boolean exists(String id)
	{
		IdentitiesMapper mapper = TransactionTL.getSql().getMapper(IdentitiesMapper.class);
		BaseBean identityTypeState = mapper.getIdentityTypeByName(id);
		return identityTypeState != null;
	}
}
