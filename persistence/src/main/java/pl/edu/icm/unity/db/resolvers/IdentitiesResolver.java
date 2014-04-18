/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.resolvers;

import java.util.List;

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
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
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
	
	public static String getComparableIdentityValue(IdentityTaV id, IdentityTypeDefinition idType)
	{
		return idType.getId() + "::" + idType.getComparableValue(id.getValue());
	}

	public String getComparableIdentityValue(IdentityTaV id) 
			throws IllegalIdentityValueException, IllegalTypeException
	{
		IdentityTypeDefinition idTypeDef = idTypesRegistry.getByName(id.getTypeId());
		if (idTypeDef == null)
			throw new IllegalIdentityValueException("The identity type is unknown");
		return getComparableIdentityValue(id, idTypeDef);
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
			String cmpVal = getComparableIdentityValue(idtavParam, idTypeDef);
			IdentityBean idBean = mapper.getIdentityByName(cmpVal);
			if (idBean == null)
			{
				if (idTypeDef.isDynamic() && idtavParam.getTarget() != null)
				{
					//TODO - only matching by type
					List<IdentityBean> allIds = mapper.getIdentities();
					String realm = getRealm();
					String target = idtavParam.getTarget();
					String toFind = idTypeDef.getComparableValue();
					for (IdentityBean idb: allIds)
					{
						Identity id = resolveIdentityBeanNoExternalize(idb, mapper);
						String externalizedValue = idTypeDef.toExternalForm(realm, target, 
								id.getValue());
						if (idTypeDef.getComparableValue(externalizedValue)
					}
				}
				throw new IllegalIdentityValueException("The entity id is invalid");
			}
			return idBean.getEntityId();
		}
	}

	public Identity resolveIdentityBeanNoExternalize(IdentityBean idB, IdentitiesMapper mapper) 
			throws IllegalTypeException
	{
		BaseBean identityTypeB = mapper.getIdentityTypeById(idB.getTypeId());
		if (identityTypeB == null)
			throw new IllegalTypeException("The identity type " + idB.getTypeId() + " is unknown");
		IdentityType idType = resolveIdentityType(identityTypeB);
		Identity ret = new Identity();
		ret.setType(idType);
		ret.setTypeId(idType.getIdentityTypeProvider().getId());
		ret.setEntityId(idB.getEntityId());
		idSerializer.fromJson(idB.getContents(), ret);
		return ret;
	}
	
	private String getRealm()
	{
		try
		{
			InvocationContext context = InvocationContext.getCurrent();
			if (context.getLoginSession() != null)
				return context.getLoginSession().getRealm();
		} catch (InternalException e)
		{
			//OK
		}
		return null;
	}
	
	public Identity resolveIdentityBean(IdentityBean idB, IdentitiesMapper mapper, String target, 
			boolean allowCreate) throws IllegalTypeException
	{
		Identity ret = resolveIdentityBeanNoExternalize(idB, mapper);
		IdentityTypeDefinition idTypeImpl = ret.getType().getIdentityTypeProvider();
		
		String realm = getRealm();
		
		String externalizedValue = idTypeImpl.toExternalForm(realm, target, ret.getValue());
		if (externalizedValue != null)
		{
			ret.setValue(externalizedValue);
			return ret;
		}
		
		if (allowCreate && idTypeImpl.isDynamic())
		{
			try
			{
				String updated = idTypeImpl.createNewIdentity(realm, target, ret.getValue());
				if (updated != null)
				{
					ret.setValue(updated);
					idB.setContents(idSerializer.toJson(ret));
					mapper.updateIdentity(idB);
				}
				
				externalizedValue = idTypeImpl.toExternalForm(realm, target, ret.getValue());
				if (externalizedValue != null)
				{
					ret.setValue(externalizedValue);
					return ret;
				}
				return null;
			} catch (IllegalTypeException e)
			{
				return null;
			}
		}
		return null;
	}
}
