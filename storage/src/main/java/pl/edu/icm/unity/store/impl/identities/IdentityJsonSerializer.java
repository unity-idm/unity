/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identities;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.hz.JsonSerializerForKryo;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


/**
 * Identity JSON and DB serialization
 * @author K. Benedyczak
 */
@Component
public class IdentityJsonSerializer implements RDBMSObjectSerializer<Identity, IdentityBean>, 
		JsonSerializerForKryo<Identity>
{
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private IdentityTypeDAO idTypeDAO;
	
	@Override
	public Identity fromJson(ObjectNode src)
	{
		String type = src.get("typeId").asText();
		long entityKey = src.get("entityId").asLong();
		IdentityType idType = idTypeDAO.get(type);
		Identity ret = new Identity();
		ret.setType(idType);
		ret.setTypeId(idType.getIdentityTypeProvider().getId());
		ret.setEntityId(entityKey);
		fromJsonBase(src, ret);
		return ret;
	}

	@Override
	public ObjectNode toJson(Identity src)
	{
		ObjectNode main = toJsonBase(src);
		main.put("typeId", src.getTypeId());
		main.put("entityId", src.getEntityId());
		return main;
	}

	@Override
	public IdentityBean toDB(Identity object)
	{
		IdentityBean idB = new IdentityBean();
		idB.setEntityId(object.getEntityId());
		idB.setName(object.getComparableValue());
		long typeKey = idTypeDAO.getKeyForName(object.getTypeId());
		idB.setTypeId(typeKey);
		idB.setContents(JsonUtil.serialize2Bytes(toJsonBase(object)));
		return idB;
	}

	@Override
	public Identity fromDB(IdentityBean bean)
	{
		IdentityType idType = idTypeDAO.getByKey(bean.getTypeId());
		Identity ret = new Identity();
		ret.setType(idType);
		ret.setTypeId(idType.getIdentityTypeProvider().getId());
		ret.setEntityId(bean.getEntityId());
		fromJsonBase(JsonUtil.parse(bean.getContents()), ret);
		return ret;
	}

	
	public ObjectNode toJsonBase(Identity src)
	{
		ObjectNode main = mapper.createObjectNode();
		if (src.getCreationTs() != null)
			main.put("creationTs", src.getCreationTs().getTime());
		if (src.getUpdateTs() != null)
			main.put("updateTs", src.getUpdateTs().getTime());
		
		if (src.getRemoteIdp() != null)
			main.put("remoteIdp", src.getRemoteIdp());
		if (src.getTranslationProfile() != null)
			main.put("translationProfile", src.getTranslationProfile());

		if (src.getValue() != null)
			main.put("value", src.getValue());
		if (src.getRealm() != null)
			main.put("realm", src.getRealm());
		if (src.getTarget() != null)
			main.put("target", src.getTarget());
		if (src.getConfirmationInfo() != null)
			main.put("confirmationInfo", src.getConfirmationInfo().getSerializedConfiguration());
		if (src.getMetadata() != null)
			main.set("metadata", src.getMetadata());
		return main;
	}
	
	public void fromJsonBase(ObjectNode main, Identity target)
	{
		if (main.has("creationTs"))
			target.setCreationTs(new Date(main.get("creationTs").asLong()));
		if (main.has("updateTs"))
			target.setUpdateTs(new Date(main.get("updateTs").asLong()));
		if (main.has("translationProfile"))
			target.setTranslationProfile(main.get("translationProfile").asText());
		if (main.has("remoteIdp"))
			target.setRemoteIdp(main.get("remoteIdp").asText());
		if (main.has("translationProfile"))
			target.setTranslationProfile(main.get("translationProfile").asText());
		if (main.has("value"))
			target.setValue(main.get("value").asText());
		if (main.has("realm"))
			target.setRealm(main.get("realm").asText());
		if (main.has("target"))
			target.setTarget(main.get("target").asText());
		if (main.has("confirmationInfo"))
		{
			ConfirmationInfo conData = new ConfirmationInfo();
			conData.setSerializedConfiguration(main.get("confirmationInfo").asText());
			target.setConfirmationInfo(conData);
		}
		if (main.has("metadata"))
			target.setMetadata(main.get("metadata"));
	}

	@Override
	public Class<? extends Identity> getClazz()
	{
		return Identity.class;
	}
}
