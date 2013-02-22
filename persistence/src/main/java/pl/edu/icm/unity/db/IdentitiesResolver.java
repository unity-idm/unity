/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.json.JsonSerializer;
import pl.edu.icm.unity.db.json.SerializersRegistry;
import pl.edu.icm.unity.db.mapper.IdentitiesMapper;
import pl.edu.icm.unity.db.model.BaseBean;
import pl.edu.icm.unity.db.model.IdentityBean;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.types.EntityParam;
import pl.edu.icm.unity.types.Identity;
import pl.edu.icm.unity.types.IdentityParam;
import pl.edu.icm.unity.types.IdentityTaV;
import pl.edu.icm.unity.types.IdentityType;
import pl.edu.icm.unity.types.IdentityTypeDefinition;

/**
 * Allows to resolve IdentityTypes, entities and Identities
 * @author K. Benedyczak
 */
@Component
public class IdentitiesResolver
{
	private JsonSerializer<IdentityType> idTypeSerializer;
	private JsonSerializer<IdentityParam> idSerializer;
	private IdentityTypesRegistry idTypesRegistry;
	
	@Autowired
	public IdentitiesResolver(SerializersRegistry serializersReg,
			IdentityTypesRegistry idTypesRegistry)
	{
		this.idTypeSerializer = serializersReg.getSerializer(IdentityType.class);
		this.idSerializer = serializersReg.getSerializer(IdentityParam.class);
		this.idTypesRegistry = idTypesRegistry;
	}

	public IdentityType resolveIdentityType(BaseBean raw)
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
	{
		IdentityTypeDefinition idTypeDef = idTypesRegistry.getByName(id.getTypeId());
		if (idTypeDef == null)
			throw new IllegalIdentityValueException("The identity type is unknown");
		return getComparableIdentityValue(id, idTypeDef);
	}
	
	public long getEntityId(EntityParam entityParam, SqlSession sqlMap)
	{
		IdentitiesMapper mapper = sqlMap.getMapper(IdentitiesMapper.class);
		BaseBean entityB;
		if (entityParam.getEntityId() != null)
		{
			long id;
			try
			{
				id = Long.parseLong(entityParam.getEntityId());
			} catch(NumberFormatException e)
			{
				throw new IllegalIdentityValueException("The entity id is invalid");
			}
			entityB = mapper.getEntityById(id);
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
	
	public Identity resolveIdentityBean(IdentityBean idB, IdentitiesMapper mapper)
	{
		BaseBean identityTypeB = mapper.getIdentityTypeById(idB.getTypeId());
		IdentityType idType = resolveIdentityType(identityTypeB);
		Identity ret = new Identity();
		ret.setType(idType);
		ret.setTypeId(idType.getIdentityTypeProvider().getId());
		ret.setEntityId(idB.getEntityId()+"");
		idSerializer.fromJson(idB.getContents(), ret);
		return ret;
	}
}
