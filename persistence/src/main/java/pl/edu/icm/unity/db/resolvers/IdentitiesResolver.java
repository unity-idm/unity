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
			String cmpVal = getComparableIdentityValue(entityParam.getIdentity());
			IdentityBean idBean = mapper.getIdentityByName(cmpVal);
			if (idBean == null)
				throw new IllegalIdentityValueException("The entity id is invalid");
			return idBean.getEntityId();
		}
	}
	
	public Identity resolveIdentityBean(IdentityBean idB, IdentitiesMapper mapper) throws IllegalTypeException
	{
		BaseBean identityTypeB = mapper.getIdentityTypeById(idB.getTypeId());
		IdentityType idType = resolveIdentityType(identityTypeB);
		Identity ret = new Identity();
		ret.setType(idType);
		ret.setTypeId(idType.getIdentityTypeProvider().getId());
		ret.setEntityId(idB.getEntityId());
		idSerializer.fromJson(idB.getContents(), ret);
		return ret;
	}
}
