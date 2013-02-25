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
import pl.edu.icm.unity.db.model.IdentityBean;
import pl.edu.icm.unity.db.resolvers.IdentitiesResolver;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.types.Entity;
import pl.edu.icm.unity.types.EntityParam;
import pl.edu.icm.unity.types.Identity;
import pl.edu.icm.unity.types.IdentityParam;
import pl.edu.icm.unity.types.IdentityTaV;
import pl.edu.icm.unity.types.IdentityType;
import pl.edu.icm.unity.types.IdentityTypeDefinition;

/**
 * Identities related DB operations
 * @author K. Benedyczak
 */
@Component
public class DBIdentities
{
	private DBLimits limits;
	private JsonSerializer<IdentityParam> idSerializer;
	private IdentityTypesRegistry idTypesRegistry;
	private IdentitiesResolver idResolver;
	
	@Autowired
	public DBIdentities(DB db, SerializersRegistry serializersReg, 
			IdentityTypesRegistry idTypesRegistry,
			IdentitiesResolver idResolver)
	{
		this.limits = db.getDBLimits();
		this.idSerializer = serializersReg.getSerializer(IdentityParam.class);
		this.idTypesRegistry = idTypesRegistry;
		this.idResolver = idResolver;
	}

	public List<IdentityType> getIdentityTypes(SqlSession sqlMap)
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		List<BaseBean> identityTypeState = mapper.getIdentityTypes();
		List<IdentityType> ret = new ArrayList<IdentityType>(identityTypeState.size());
		for (BaseBean state: identityTypeState)
		{
			ret.add(idResolver.resolveIdentityType(state));
		}
		return ret;
	}	

	public Identity insertIdentity(IdentityParam toAdd, Long entityId, SqlSession sqlMap)
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		
		IdentityTypeDefinition idTypeDef = idTypesRegistry.getByName(toAdd.getTypeId());
		if (idTypeDef == null)
			throw new IllegalIdentityValueException("The identity type is unknown");
		BaseBean identityTypeB = mapper.getIdentityTypeByName(idTypeDef.getId());
		if (identityTypeB == null)
			throw new RuntimeEngineException("The identity type id is not stored in the database: " + 
					idTypeDef.getId());
		idTypeDef.validate(toAdd.getValue());
		String cmpVal = IdentitiesResolver.getComparableIdentityValue(toAdd, idTypeDef);
		if (mapper.getIdentityByName(cmpVal) != null)
			throw new IllegalIdentityValueException("The identity with this value is already present");
		if (cmpVal.length() > limits.getNameLimit())
			throw new IllegalGroupValueException("Identity comparable value length must not exceed " + 
					limits.getNameLimit() + " characters");
		
		if (entityId == null)
		{
			BaseBean entityB = new BaseBean();
			mapper.insertEntity(entityB);
			entityId = entityB.getId();
		}
		
		IdentityBean idB = new IdentityBean();
		idB.setEntityId(entityId);
		idB.setName(cmpVal);
		idB.setTypeId(identityTypeB.getId());
		idB.setContents(idSerializer.toJson(toAdd));
		mapper.insertIdentity(idB);
		
		IdentityType idType = idResolver.resolveIdentityType(identityTypeB);
		return new Identity(idType, toAdd.getValue(), entityId+"", toAdd.isEnabled(), toAdd.isLocal());
	}

	
	public Entity getIdentitiesForEntity(long entityId, SqlSession sqlMap)
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		List<IdentityBean> rawRet = mapper.getIdentitiesByEntity(entityId);
		Identity[] identities = new Identity[rawRet.size()];
		for (int i=0; i<identities.length; i++)
			identities[i] = idResolver.resolveIdentityBean(rawRet.get(i), mapper);
		return new Entity(entityId+"", identities);
	}
	
	public void setIdentityStatus(IdentityTaV toChange, boolean status, SqlSession sqlMap)
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		String cmpVal = idResolver.getComparableIdentityValue(toChange);
		IdentityBean idBean = mapper.getIdentityByName(cmpVal); 
		if (idBean == null)
			throw new IllegalIdentityValueException("The identity does not exist");

		Identity tmp = new Identity();
		idSerializer.fromJson(idBean.getContents(), tmp);
		tmp.setEnabled(status);
		
		IdentityBean idBeanUpdated = new IdentityBean();
		idBeanUpdated.setContents(idSerializer.toJson(tmp));
		idBeanUpdated.setId(idBean.getId());
		mapper.updateIdentity(idBeanUpdated);
	}
	
	public void removeIdentity(IdentityTaV toRemove, SqlSession sqlMap)
	{
		IdentityTypeDefinition idTypeDef = idTypesRegistry.getByName(toRemove.getTypeId());
		if (idTypeDef == null)
			throw new IllegalIdentityValueException("The identity type is unknown");
		if (idTypeDef.isSystem())
			throw new IllegalIdentityValueException("The system identity can not be removed without removing the containing entity.");
		
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		String cmpVal = idResolver.getComparableIdentityValue(toRemove);
		IdentityBean idBean = mapper.getIdentityByName(cmpVal); 
		if (idBean == null)
			throw new IllegalIdentityValueException("The identity does not exist");
		
		mapper.deleteIdentity(cmpVal);
	}
	
	public void removeEntity(EntityParam toRemove, SqlSession sqlMap)
	{
		long entityId = idResolver.getEntityId(toRemove, sqlMap);
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		mapper.deleteEntity(entityId);
	}
}




