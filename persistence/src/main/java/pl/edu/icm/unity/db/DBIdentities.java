/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
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
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.EntityInformation;
import pl.edu.icm.unity.types.EntityScheduledOperation;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityRepresentation;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

/**
 * Identities related DB operations
 * @author K. Benedyczak
 */
@Component
public class DBIdentities
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, DBIdentities.class);
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
	 * Insert a new entity. The entity may have its id set or not. In the latter case the id is chosen by the DB
	 * and set in the input object.
	 * @param entityB
	 * @param sqlMap
	 */
	public void insertEntity(BaseBean entityB, SqlSession sqlMap)
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		if (entityB.getId() == null)
			mapper.insertEntity(entityB);
		else
			mapper.insertEntityWithId(entityB);
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
	public Identity insertIdentity(IdentityParam toAdd, Long entityId, boolean allowSystem, SqlSession sqlMap) 
			throws IllegalIdentityValueException, IllegalTypeException, WrongArgumentException
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		
		IdentityTypeDefinition idTypeDef = idTypesRegistry.getByName(toAdd.getTypeId());
		if (idTypeDef == null)
			throw new IllegalIdentityValueException("The identity type is unknown");
		if (idTypeDef.isDynamic() && !allowSystem)
			throw new IllegalIdentityValueException("The identity type " + idTypeDef.getId() + 
					" is created automatically and can not be added manually");
		BaseBean identityTypeB = mapper.getIdentityTypeByName(idTypeDef.getId());
		if (identityTypeB == null)
			throw new InternalException("The identity type id is not stored in the database: " + 
					idTypeDef.getId());
		idTypeDef.validate(toAdd.getValue());
		if (idTypeDef.isTargeted())
		{
			if (toAdd.getTarget() == null || toAdd.getRealm() == null)
				throw new IllegalIdentityValueException("The identity target and realm are required "
						+ "for identity type " + idTypeDef.getId());
		} else
		{
			if (toAdd.getTarget() != null || toAdd.getRealm() != null)
				throw new IllegalIdentityValueException("The identity target and realm must not be set "
						+ "for identity type " + idTypeDef.getId());
		}
		String cmpVal = IdentitiesResolver.getComparableIdentityValue(toAdd, idTypeDef);
		if (mapper.getIdentityByName(cmpVal) != null)
			throw new IllegalIdentityValueException("The identity with this value is already present");
		limits.checkNameLimit(cmpVal);
		
		if (entityId == null)
		{
			BaseBean entityB = new BaseBean();
			insertEntity(entityB, sqlMap);
			entityId = entityB.getId();
		}
		
		IdentityBean idB = new IdentityBean();
		idB.setEntityId(entityId);
		idB.setName(cmpVal);
		idB.setTypeId(identityTypeB.getId());
		Date ts = new Date();
		idB.setContents(idSerializer.toJson(toAdd, ts, ts));
		mapper.insertIdentity(idB);
		
		IdentityType idType = idResolver.resolveIdentityType(identityTypeB);
		return new Identity(idType, toAdd.getValue(), entityId, toAdd.getRealm(), toAdd.getTarget(),
				toAdd.getRemoteIdp(), toAdd.getTranslationProfile(), ts, ts, toAdd.getConfirmationInfo());
	}
	
	public Identity updateIdentityConfirmationInfo(IdentityTaV idTav, ConfirmationInfo newConfirmation,
			SqlSession sqlMap) throws IllegalTypeException, IllegalIdentityValueException
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		IdentityTypeDefinition idTypeDef = idTypesRegistry.getByName(idTav.getTypeId());
		if (idTypeDef == null)
			throw new IllegalIdentityValueException("The identity type is unknown");
		String cmpVal = IdentitiesResolver.getComparableIdentityValue(idTav, idTypeDef);
		IdentityBean idBean = mapper.getIdentityByName(cmpVal);
		if (idBean == null)
			throw new IllegalIdentityValueException("The identity with this value is not available in db");
		
		Identity resolved = idResolver.resolveIdentityBeanNoExternalize(idBean, mapper);
		resolved.setConfirmationInfo(newConfirmation);
		IdentityBean idB = new IdentityBean();
		idB.setEntityId(idBean.getEntityId());
		idB.setName(cmpVal);
		idB.setTypeId(idBean.getTypeId());
		Date ts = new Date();
		idB.setContents(idSerializer.toJson(resolved, resolved.getCreationTs(), ts));
		mapper.updateIdentity(idB);
		return resolved;
	}
	
	public Identity[] getIdentitiesForEntity(long entityId, String target, boolean allowCreate, SqlSession sqlMap) 
			throws IllegalTypeException
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		List<IdentityBean> rawRet = mapper.getIdentitiesByEntity(entityId);
		List<Identity> ret = new ArrayList<Identity>(rawRet.size());
		Set<String> presentTypes = new HashSet<String>();
		for (int i=0; i<rawRet.size(); i++)
		{
			Identity id = idResolver.resolveIdentityBean(rawRet.get(i), mapper, target);
			if (id != null)
			{
				ret.add(id);
				presentTypes.add(id.getTypeId());
			}
		}
		if (allowCreate)
			addDynamic(entityId, presentTypes, ret, target, mapper);
		return ret.toArray(new Identity[ret.size()]);
	}

	public Identity[] getIdentitiesForEntityNoContext(long entityId, SqlSession sqlMap) 
			throws IllegalTypeException
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		List<IdentityBean> rawRet = mapper.getIdentitiesByEntity(entityId);
		List<Identity> ret = new ArrayList<Identity>(rawRet.size());
		for (int i=0; i<rawRet.size(); i++)
		{
			Identity id = idResolver.resolveIdentityBeanNoContext(rawRet.get(i), mapper);
			if (id != null)
				ret.add(id);
		}
		return ret.toArray(new Identity[ret.size()]);
	}
	
	public void removeExpiredIdentities(SqlSession sqlMap)
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		List<IdentityBean> rawRet = mapper.getIdentities();
		for (IdentityBean identityBean: rawRet)
		{
			Identity resolved;
			try
			{
				resolved = idResolver.resolveIdentityBeanNoExternalize(identityBean, mapper);
			} catch (IllegalTypeException e)
			{
				log.error("Can't resolve an identity stored in DB", e);
				continue;
			}
			IdentityRepresentation idRepresentation = new IdentityRepresentation(identityBean.getName(), 
					resolved.getValue());
			if (resolved.getType().getIdentityTypeProvider().isExpired(idRepresentation))
			{
				log.debug("Removing expired identity " + resolved);
				mapper.deleteIdentity(identityBean.getName());
			}
		}
	}
	
	public boolean isIdentityConfirmed(SqlSession sqlMap, IdentityTaV tav) 
			throws IllegalTypeException, IllegalIdentityValueException
	{
		IdentityTypeDefinition idTypeDef = idTypesRegistry.getByName(tav.getTypeId());
		if (!idTypeDef.isVerifiable())
			return true;
		String cmpVal = IdentitiesResolver.getComparableIdentityValue(tav, idTypeDef);
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		IdentityBean idBean = mapper.getIdentityByName(cmpVal);
		Identity resolved = idResolver.resolveIdentityBeanNoExternalize(idBean, mapper);
		return resolved.getConfirmationInfo().isConfirmed();
	}
	
	/**
	 * Creates dynamic identities which are currently absent for the entity.
	 */
	private void addDynamic(long entityId, Set<String> presentTypes, List<Identity> ret, String target, 
			IdentitiesMapper mapper)
	{
		for (IdentityTypeDefinition idType: idTypesRegistry.getDynamic())
		{
			if (presentTypes.contains(idType.getId()))
				continue;
			if (idType.isTargeted() && target == null)
				continue;
			Identity added = idResolver.createDynamicIdentity(idType, entityId, mapper, target);
			if (added != null)
				ret.add(added);
		}
	}
	
	public void resetIdentityForEntity(long entityId, String type, String realm, String target, SqlSession sqlMap) 
			throws IllegalTypeException
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		List<IdentityBean> rawRet = mapper.getIdentitiesByEntity(entityId);
		for (IdentityBean idBean: rawRet)
		{
			Identity id = idResolver.resolveIdentityBeanNoExternalize(idBean, mapper);
			if (id.getTypeId().equals(type))
			{
				IdentityTypeDefinition idTypeImpl = id.getType().getIdentityTypeProvider();
				if (!idTypeImpl.isDynamic())
					throw new IllegalTypeException("Reset is possible for dynamic identity types only");
				if (realm != null && !realm.equals(id.getRealm()))
					continue;
				if (target != null && !target.equals(id.getTarget()))
					continue;
				mapper.deleteIdentity(idBean.getName());
			}
		}
	}
	
	public EntityInformation getEntityInformation(long entityId, SqlSession sqlMap) 
			throws IllegalIdentityValueException, IllegalTypeException
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		BaseBean bean = mapper.getEntityById(entityId);
		return entitySerializer.fromJson(bean.getContents());
	}

	public EntityState getEntityStatus(long entityId, SqlSession sqlMap) 
			throws IllegalIdentityValueException, IllegalTypeException
	{
		return getEntityInformation(entityId, sqlMap).getState();
	}
	
	public void setEntityStatus(long entityId, EntityState status, SqlSession sqlMap) 
			throws IllegalIdentityValueException, IllegalTypeException
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		BaseBean bean = mapper.getEntityById(entityId);
		EntityInformation info = entitySerializer.fromJson(bean.getContents());
		info.setState(status);
		byte[] infoJson = entitySerializer.toJson(info);
		bean.setContents(infoJson);
		mapper.updateEntity(bean);
	}
	
	/**
	 * If entity is in the state {@link EntityState#onlyLoginPermitted} this method clears the 
	 *  removal of the entity: state is set to enabled and user ordered removal is removed.
	 * @param entityId
	 * @param sqlMap
	 * @throws IllegalIdentityValueException
	 * @throws IllegalTypeException
	 */
	public void clearScheduledRemovalStatus(long entityId, SqlSession sqlMap) 
			throws IllegalIdentityValueException, IllegalTypeException
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		BaseBean bean = mapper.getEntityById(entityId);
		EntityInformation info = entitySerializer.fromJson(bean.getContents());
		if (info.getState() != EntityState.onlyLoginPermitted)
			return;
		log.debug("Removing scheduled removal of an account [as the user is being logged] for entity " + 
			entityId);
		info.setState(EntityState.valid);
		info.setRemovalByUserTime(null);
		byte[] infoJson = entitySerializer.toJson(info);
		bean.setContents(infoJson);
		mapper.updateEntity(bean);
	}

	public void setScheduledOperationByAdmin(long entityId, Date when,
			EntityScheduledOperation operation, SqlSession sqlMap) 
			throws IllegalIdentityValueException, IllegalTypeException
	{
		if (operation != null && when == null)
			throw new IllegalArgumentException("Date must be set for the scheduled operation");
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		BaseBean bean = mapper.getEntityById(entityId);
		EntityInformation info = entitySerializer.fromJson(bean.getContents());

		if (operation == null)
		{
			info.setScheduledOperation(null);
			info.setScheduledOperationTime(null);
		} else
		{
			info.setScheduledOperation(operation);
			info.setScheduledOperationTime(when);
		}

		byte[] infoJson = entitySerializer.toJson(info);
		bean.setContents(infoJson);
		mapper.updateEntity(bean);
	}

	public void setScheduledRemovalByUser(long entityId, Date when, SqlSession sqlMap) 
			throws IllegalIdentityValueException, IllegalTypeException
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		BaseBean bean = mapper.getEntityById(entityId);
		EntityInformation info = entitySerializer.fromJson(bean.getContents());

		if (when == null)
		{
			info.setRemovalByUserTime(null);
		} else
		{
			info.setRemovalByUserTime(when);
			info.setState(EntityState.onlyLoginPermitted);
		}

		byte[] infoJson = entitySerializer.toJson(info);
		bean.setContents(infoJson);
		mapper.updateEntity(bean);
	}
	
	public void removeIdentity(IdentityTaV toRemove, SqlSession sqlMap) throws IllegalIdentityValueException, 
		IllegalTypeException
	{
		IdentityTypeDefinition idTypeDef = idTypesRegistry.getByName(toRemove.getTypeId());
		if (idTypeDef == null)
			throw new IllegalIdentityValueException("The identity type is unknown");
		
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		String cmpVal = IdentitiesResolver.getComparableIdentityValue(toRemove, idTypeDef);
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
	
	/**
	 * Performs all scheduled operations due by now
	 * @param sqlMap
	 * @return the time when the earliest scheduled operation should take place. If there is no such operation 
	 * returned time is very far in future.
	 */
	public Date performScheduledEntityOperations(SqlSession sqlMap)
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		List<BaseBean> rawRet = mapper.getEntities();
		Date now = new Date();
		Date ret = new Date(Long.MAX_VALUE);
		for (BaseBean entityBean: rawRet)
		{
			EntityInformation entityInfo = entitySerializer.fromJson(entityBean.getContents());
			if (isSetAndAfter(now, entityInfo.getScheduledOperationTime()))
			{
				EntityScheduledOperation op = entityInfo.getScheduledOperation();
				performScheduledOperationInternal(entityBean, op, entityInfo, mapper);
			} else if (isSetAndAfter(now, entityInfo.getRemovalByUserTime()))
			{
				performScheduledOperationInternal(entityBean, 
						EntityScheduledOperation.REMOVE, entityInfo, mapper);
			}
			
			Date nextOp = entityInfo.getScheduledOperationTime();
			if (nextOp != null && nextOp.before(ret))
				ret = nextOp;
		}
		return ret;
	}

	public void performScheduledOperation(long entityId, EntityScheduledOperation op, SqlSession sqlMap)
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		BaseBean entityBean = mapper.getEntityById(entityId);
		EntityInformation entityInfo = entitySerializer.fromJson(entityBean.getContents());
		performScheduledOperationInternal(entityBean, op, entityInfo, mapper);
	}
	
	private void performScheduledOperationInternal(BaseBean entityBean, EntityScheduledOperation op,
			EntityInformation entityInfo, IdentitiesMapper mapper)
	{
		switch (op)
		{
		case DISABLE:
			log.info("Performing scheduled disable of entity " + entityBean.getId());
			disableInternal(entityInfo, entityBean, mapper);
			break;
		case REMOVE:
			log.info("Performing scheduled removal of entity " + entityBean.getId());
			mapper.deleteEntity(entityBean.getId());
			break;
		}
	}
	
	private void disableInternal(EntityInformation entityInfo, BaseBean entityBean, IdentitiesMapper mapper)
	{
		entityInfo.setState(EntityState.disabled);
		entityInfo.setScheduledOperation(null);
		entityInfo.setScheduledOperationTime(null);
		entityInfo.setRemovalByUserTime(null);
		byte[] infoJson = entitySerializer.toJson(entityInfo);
		entityBean.setContents(infoJson);
		mapper.updateEntity(entityBean);
	}
	
	private boolean isSetAndAfter(Date now, Date date)
	{
		return date != null && !now.before(date);
	}
}





