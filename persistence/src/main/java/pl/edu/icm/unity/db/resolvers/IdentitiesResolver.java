/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.resolvers;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.json.IdentitySerializer;
import pl.edu.icm.unity.db.json.IdentityTypeSerializer;
import pl.edu.icm.unity.db.mapper.IdentitiesMapper;
import pl.edu.icm.unity.db.model.BaseBean;
import pl.edu.icm.unity.db.model.IdentityBean;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityRepresentation;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;

/**
 * Allows to resolve IdentityTypes, entities and Identities
 * @author K. Benedyczak
 */
@Component
public class IdentitiesResolver
{
	private IdentityTypeSerializer idTypeSerializer;
	private IdentitySerializer idSerializer;
	private IdentityTypesRegistry idTypesRegistry;
	
	@Autowired
	public IdentitiesResolver(IdentityTypeSerializer idTypeSerializer,
			IdentitySerializer idSerializer,
			IdentityTypesRegistry idTypesRegistry)
	{
		this.idTypeSerializer = idTypeSerializer;
		this.idSerializer = idSerializer;
		this.idTypesRegistry = idTypesRegistry;
	}


	public IdentityType resolveIdentityType(BaseBean raw) throws IllegalTypeException
	{
		IdentityType it = new IdentityType(idTypesRegistry.getByName(raw.getName()));
		idTypeSerializer.fromJson(raw.getContents(), it);
		return it;
	}
	
	public IdentityType resolveIdentityType(long id, IdentitiesMapper mapper) throws IllegalTypeException
	{
		BaseBean identityTypeB = mapper.getIdentityTypeById(id);
		if (identityTypeB == null)
			throw new IllegalTypeException("The identity type " + id + " is unknown");
		return resolveIdentityType(identityTypeB);
	}
	
	private static String toInDBIdentityValue(String typeName, String comparableTypeSpecificValue)
	{
		return typeName + "::" + comparableTypeSpecificValue;
	}
	
	public static String getComparableIdentityValue(IdentityTaV id, IdentityTypeDefinition idType)
			throws IllegalIdentityValueException
	{
		return toInDBIdentityValue(idType.getId(), idType.getComparableValue(id.getValue(), id.getRealm(), 
				id.getTarget()));
	}

	/**
	 * Algorithm is as follows:
	 * 1) if entity param has entity id set then this id is returned after checking that it is valid
	 * 2) otherwise a comparable identity value is created from the {@link IdentityTaV} parameter and
	 * searched in the database. If entity is found it is returned.
	 * 3) if entity is not found and there is target set and the identity type is dynamic an 
	 * another search is performed: all typed identities are queried, and checked one by one using the target.
	 * 
	 * @param entityParam
	 * @param sqlMap
	 * @return
	 * @throws IllegalIdentityValueException
	 * @throws IllegalTypeException
	 */
	public long getEntityId(EntityParam entityParam, SqlSession sqlMap) 
			throws IllegalIdentityValueException, IllegalTypeException
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		BaseBean entityB;
		if (entityParam.getEntityId() != null)
		{
			entityB = mapper.getEntityById(entityParam.getEntityId());
			if (entityB == null)
				throw new IllegalIdentityValueException("The entity id is invalid");
			return entityB.getId();
		} else
		{
			IdentityTaV idtavParam = entityParam.getIdentity();
			IdentityTypeDefinition idTypeDef = idTypesRegistry.getByName(idtavParam.getTypeId());
			if (idTypeDef == null)
				throw new IllegalIdentityValueException("The identity type is unknown");
			
			String inDBIdentityValue;
			if (idTypeDef.isTargeted())
			{
				if (idtavParam.getTarget() == null)
					throw new IllegalIdentityValueException("The target is mandatory "
							+ "for identity type " + idtavParam.getTypeId());
				inDBIdentityValue = getComparableIdentityValue(idtavParam, idTypeDef);
			} else
			{
				inDBIdentityValue = getComparableIdentityValue(idtavParam, idTypeDef);
			}

			IdentityBean idBean = mapper.getIdentityByName(inDBIdentityValue);
			if (idBean == null)
				throw new IllegalIdentityValueException("The entity id is invalid");
			return idBean.getEntityId();
		}
	}

	public Identity resolveIdentityBeanNoExternalize(IdentityBean idB, IdentitiesMapper mapper) 
			throws IllegalTypeException
	{
		IdentityType idType = resolveIdentityType(idB.getTypeId(), mapper);
		Identity ret = new Identity();
		ret.setType(idType);
		ret.setTypeId(idType.getIdentityTypeProvider().getId());
		ret.setEntityId(idB.getEntityId());
		idSerializer.fromJson(idB.getContents(), ret);
		return ret;
	}
	
	public Identity resolveIdentityBean(IdentityBean idB, IdentitiesMapper mapper, String target) 
			throws IllegalTypeException
	{
		Identity ret = resolveIdentityBeanNoContext(idB, mapper);
		if (ret == null)
			return null;
		IdentityTypeDefinition idTypeImpl = ret.getType().getIdentityTypeProvider();
		
		String realm = InvocationContext.safeGetRealm();
		
		if (idTypeImpl.isTargeted() && (realm == null || target == null))
			return null;
		if (target != null && ret.getTarget() != null && !ret.getTarget().equals(target))
			return null;
		if (realm != null && ret.getRealm() != null && !ret.getRealm().equals(realm))
			return null;
		
		return ret;
	}
	
	/**
	 * Resolves Identity from a bean, using its target and realm. I.e. this method works for all beans always
	 * regardless of the invocation context.
	 * @param idB
	 * @param mapper
	 * @return
	 * @throws IllegalTypeException
	 */
	public Identity resolveIdentityBeanNoContext(IdentityBean idB, IdentitiesMapper mapper) 
			throws IllegalTypeException
	{
		Identity ret = resolveIdentityBeanNoExternalize(idB, mapper);
		IdentityTypeDefinition idTypeImpl = ret.getType().getIdentityTypeProvider();
		
		String externalizedValue = idTypeImpl.toExternalFormNoContext(ret.getValue(), idB.getName());
		if (externalizedValue != null)
		{
			ret.setValue(externalizedValue);
			return ret;
		}
		return null;
	}
	
	
	
	public Identity createDynamicIdentity(IdentityTypeDefinition idTypeImpl, long entityId, 
			IdentitiesMapper mapper, String target)
	{
		String realm = InvocationContext.safeGetRealm();
		
		if (idTypeImpl.isTargeted() && (realm == null || target == null))
			return null;
		try
		{
			IdentityRepresentation newId = idTypeImpl.createNewIdentity(realm, target, null);
			IdentityBean newIdBean = new IdentityBean();
			newIdBean.setEntityId(entityId);
			newIdBean.setName(toInDBIdentityValue(idTypeImpl.getId(), newId.getComparableValue()));
			long typeId = mapper.getIdentityTypeByName(idTypeImpl.getId()).getId();
			newIdBean.setTypeId(typeId);
			IdentityParam idParam = new IdentityParam();
			idParam.setLocal(true);
			idParam.setValue(newId.getContents());
			if (idTypeImpl.isTargeted())
			{
				idParam.setRealm(realm);
				idParam.setTarget(target);
			}
			newIdBean.setContents(idSerializer.toJson(idParam));
			mapper.insertIdentity(newIdBean);
			
			String externalizedValue = idTypeImpl.toExternalForm(realm, target, newId.getContents(), 
					newIdBean.getName());
			Identity ret = resolveIdentityBeanNoExternalize(newIdBean, mapper);
			ret.setValue(externalizedValue);
			return ret;
		} catch (IllegalTypeException e)
		{
			return null;
		} catch (IllegalIdentityValueException e)
		{
			return null;
		}
	}
}
