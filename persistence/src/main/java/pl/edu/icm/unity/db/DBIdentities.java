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

import pl.edu.icm.unity.db.json.EntitySerializer;
import pl.edu.icm.unity.db.json.IdentitySerializer;
import pl.edu.icm.unity.db.json.IdentityTypeSerializer;
import pl.edu.icm.unity.db.mapper.IdentitiesMapper;
import pl.edu.icm.unity.db.model.BaseBean;
import pl.edu.icm.unity.db.model.DBLimits;
import pl.edu.icm.unity.db.model.IdentityBean;
import pl.edu.icm.unity.db.resolvers.IdentitiesResolver;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;

/**
 * Identities related DB operations
 * @author K. Benedyczak
 */
@Component
public class DBIdentities
{
	private DBLimits limits;
	private IdentitySerializer idSerializer;
	private EntitySerializer entitySerializer;
	private IdentityTypeSerializer idTypeSerializer;
	private IdentityTypesRegistry idTypesRegistry;
	private IdentitiesResolver idResolver;
	
	@Autowired
	public DBIdentities(DB db, IdentityTypesRegistry idTypesRegistry, IdentitySerializer idSerializer,
			IdentityTypeSerializer idTypeSerializer, IdentitiesResolver idResolver, 
			EntitySerializer entitySerializer)
	{
		this.limits = db.getDBLimits();
		this.idSerializer = idSerializer;
		this.idTypeSerializer = idTypeSerializer;
		this.idTypesRegistry = idTypesRegistry;
		this.idResolver = idResolver;
		this.entitySerializer = entitySerializer;
	}

	public List<IdentityType> getIdentityTypes(SqlSession sqlMap)
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		List<BaseBean> identityTypeState = mapper.getIdentityTypes();
		List<IdentityType> ret = new ArrayList<IdentityType>(identityTypeState.size());
		for (BaseBean state: identityTypeState)
		{
			try
			{
				ret.add(idResolver.resolveIdentityType(state));
			} catch (IllegalTypeException e)
			{
				throw new InternalException("Can't find implementation of the identity type " + 
						state.getName(), e);
			}
		}
		return ret;
	}	

	public void createIdentityType(SqlSession session, IdentityTypeDefinition idTypeDef)
	{
		IdentitiesMapper mapper = session.getMapper(IdentitiesMapper.class);
		BaseBean toAdd = new BaseBean();

		IdentityType idType = new IdentityType(idTypeDef);
		idType.setDescription(idTypeDef.getDefaultDescription());
		toAdd.setName(idTypeDef.getId());
		toAdd.setContents(idTypeSerializer.toJson(idType));
		mapper.insertIdentityType(toAdd);
	}

	public void updateIdentityType(SqlSession session, IdentityType idType)
	{
		IdentitiesMapper mapper = session.getMapper(IdentitiesMapper.class);
		BaseBean toUpdate = new BaseBean();

		toUpdate.setName(idType.getIdentityTypeProvider().getId());
		toUpdate.setContents(idTypeSerializer.toJson(idType));
		
		mapper.updateIdentityType(toUpdate);
	}
	
	/**
	 * 
	 * @param toAdd
	 * @param entityId can be null if a new entity should be created
	 * @param sqlMap
	 * @return
	 * @throws IllegalIdentityValueException 
	 * @throws IllegalTypeException 
	 * @throws IllegalArgumentException 
	 */
	public Identity insertIdentity(IdentityParam toAdd, Long entityId, SqlSession sqlMap) 
			throws IllegalIdentityValueException, IllegalTypeException, WrongArgumentException
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		
		IdentityTypeDefinition idTypeDef = idTypesRegistry.getByName(toAdd.getTypeId());
		if (idTypeDef == null)
			throw new IllegalIdentityValueException("The identity type is unknown");
		BaseBean identityTypeB = mapper.getIdentityTypeByName(idTypeDef.getId());
		if (identityTypeB == null)
			throw new InternalException("The identity type id is not stored in the database: " + 
					idTypeDef.getId());
		idTypeDef.validate(toAdd.getValue());
		String cmpVal = IdentitiesResolver.getComparableIdentityValue(toAdd, idTypeDef);
		if (mapper.getIdentityByName(cmpVal) != null)
			throw new IllegalIdentityValueException("The identity with this value is already present");
		limits.checkNameLimit(cmpVal);
		
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
		return new Identity(idType, toAdd.getValue(), entityId+"", toAdd.isLocal());
	}

	
	public Identity[] getIdentitiesForEntity(long entityId, SqlSession sqlMap) 
			throws IllegalTypeException
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		List<IdentityBean> rawRet = mapper.getIdentitiesByEntity(entityId);
		Identity[] identities = new Identity[rawRet.size()];
		for (int i=0; i<identities.length; i++)
			identities[i] = idResolver.resolveIdentityBean(rawRet.get(i), mapper);
		return identities;
	}

	
	public EntityState getEntityStatus(long entityId, SqlSession sqlMap) 
			throws IllegalIdentityValueException, IllegalTypeException
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		BaseBean bean = mapper.getEntityById(entityId);
		return entitySerializer.fromJson(bean.getContents());
	}
	
	public void setEntityStatus(long entityId, EntityState status, SqlSession sqlMap) 
			throws IllegalIdentityValueException, IllegalTypeException
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		byte[] statusJson = entitySerializer.toJson(status);
		
		BaseBean bean = mapper.getEntityById(entityId);
		bean.setContents(statusJson);
		mapper.updateEntity(bean);
	}
	
	public void removeIdentity(IdentityTaV toRemove, SqlSession sqlMap) throws IllegalIdentityValueException, IllegalTypeException
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
	
	public void removeEntity(long entityId, SqlSession sqlMap)
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		mapper.deleteEntity(entityId);
	}
}




