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

import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.rdbms.mapper.IdentitiesMapper;
import pl.edu.icm.unity.store.rdbms.model.BaseBean;
import pl.edu.icm.unity.store.tx.SqlSessionTL;
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
	private IdentityTypeJSONSerializer idTypeSerializer;
	
	@Override
	public Map<String, IdentityType> getIdentityTypes()
	{
		IdentitiesMapper mapper = SqlSessionTL.get().getMapper(IdentitiesMapper.class);
		List<BaseBean> identityTypeState = mapper.getIdentityTypes();
		Map<String, IdentityType> ret = new HashMap<>(identityTypeState.size());
		for (BaseBean state: identityTypeState)
		{
			try
			{
				ret.put(state.getName(), idTypeSerializer.resolveIdentityType(state));
			} catch (IllegalTypeException e)
			{
				throw new InternalException("Can't find implementation of the identity type " + 
						state.getName(), e);
			}
		}
		return ret;
	}

	@Override
	public void createIdentityType(IdentityType idType)
	{
		IdentitiesMapper mapper = SqlSessionTL.get().getMapper(IdentitiesMapper.class);
		BaseBean toAdd = idTypeSerializer.serialize(idType);
		mapper.insertIdentityType(toAdd);
	}

	@Override
	public void updateIdentityType(IdentityType idType)
	{
		IdentitiesMapper mapper = SqlSessionTL.get().getMapper(IdentitiesMapper.class);
		BaseBean toUpdate = idTypeSerializer.serialize(idType);
		mapper.updateIdentityType(toUpdate);
	}

	@Override
	public void deleteIdentityType(String idType)
	{
		IdentitiesMapper mapper = SqlSessionTL.get().getMapper(IdentitiesMapper.class);
		mapper.deleteIdentityType(idType);
	}

	@Override
	public IdentityType getIdentityType(String idType)
	{
		IdentitiesMapper mapper = SqlSessionTL.get().getMapper(IdentitiesMapper.class);
		BaseBean identityTypeState = mapper.getIdentityTypeByName(idType);
		try
		{
			return idTypeSerializer.resolveIdentityType(identityTypeState);
		} catch (IllegalTypeException e)
		{
			throw new InternalException("Can't find implementation of the identity type " + 
					idType, e);
		}
	}
}
