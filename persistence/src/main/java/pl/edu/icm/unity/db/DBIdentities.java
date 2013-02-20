/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.json.JsonSerializer;
import pl.edu.icm.unity.db.json.SerializersRegistry;
import pl.edu.icm.unity.db.mapper.IdentitiesMapper;
import pl.edu.icm.unity.db.model.BaseBean;
import pl.edu.icm.unity.db.model.DBLimits;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.types.IdentityType;

/**
 * Identities related DB operations
 * @author K. Benedyczak
 */
@Component
public class DBIdentities
{
	private DBLimits limits;
	private JsonSerializer<IdentityType> jsonS;
	private IdentityTypesRegistry idTypesRegistry;
	
	@Autowired
	public DBIdentities(DB db, SerializersRegistry serializersReg, 
			IdentityTypesRegistry idTypesRegistry)
	{
		this.limits = db.getDBLimits();
		this.jsonS = serializersReg.getSerializer(IdentityType.class);
		this.idTypesRegistry = idTypesRegistry;
	}

	public List<IdentityType> getIdentityTypes(SqlSession sqlMap)
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		List<BaseBean> identityTypeState = mapper.getIdentityTypes();
		List<IdentityType> ret = new ArrayList<IdentityType>(identityTypeState.size());
		for (BaseBean state: identityTypeState)
		{
			IdentityType it = new IdentityType(idTypesRegistry.getByName(state.getName()));
			jsonS.fromJson(state.getContents(), it);
			ret.add(it);
		}
		return ret;
	}
	
	public void insertIdentityType(IdentityType toAdd, SqlSession sqlMap)
	{
		if (toAdd.getIdentityTypeProvider().getId().length() > limits.getNameLimit())
			throw new IllegalGroupValueException("Identity type name length must not exceed " + 
					limits.getNameLimit() + " characters");

		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		BaseBean arg = new BaseBean();
		arg.setContents(jsonS.toJson(toAdd));
		
		arg.setName(toAdd.getIdentityTypeProvider().getId());
		mapper.insertIdentityType(arg);
	}
}
