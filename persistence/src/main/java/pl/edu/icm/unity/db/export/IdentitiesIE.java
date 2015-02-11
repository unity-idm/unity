/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.export;

import java.io.IOException;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.mapper.IdentitiesMapper;
import pl.edu.icm.unity.db.model.BaseBean;
import pl.edu.icm.unity.db.model.IdentityBean;
import pl.edu.icm.unity.db.resolvers.IdentitiesResolver;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.stdext.identity.TargetedPersistentIdentity;
import pl.edu.icm.unity.stdext.identity.TransientIdentity;
import pl.edu.icm.unity.types.basic.Identity;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handles import/export of identities table.
 * @author K. Benedyczak
 */
@Component
public class IdentitiesIE extends AbstractIE
{
	private final DBIdentities dbIdentities;
	private final IdentitiesResolver idResolver;
	
	@Autowired
	public IdentitiesIE(ObjectMapper jsonMapper, DBIdentities dbIdentities,
			IdentitiesResolver idResolver)
	{
		super(jsonMapper);
		this.dbIdentities = dbIdentities;
		this.idResolver = idResolver;
	}

	public void serialize(SqlSession sql, JsonGenerator jg) throws JsonGenerationException, 
		IOException, IllegalTypeException
	{
		IdentitiesMapper mapper = sql.getMapper(IdentitiesMapper.class);
		List<IdentityBean> beans = mapper.getIdentities();
		jg.writeStartArray();
		for (IdentityBean bean: beans)
		{
			jg.writeStartObject();
			serializeToJson(jg, bean, mapper);
			jg.writeEndObject();
		}
		jg.writeEndArray();
	}
	
	public void deserialize(SqlSession sql, JsonParser input, DumpHeader header) throws IOException, EngineException
	{
		
		IdentitiesMapper mapper = sql.getMapper(IdentitiesMapper.class);
		JsonUtils.expect(input, JsonToken.START_ARRAY);
		while(input.nextToken() == JsonToken.START_OBJECT)
		{
			IdentityBean bean = new IdentityBean();
			super.deserializeBaseBeanFromJson(input, bean);
			JsonUtils.nextExpect(input, "entityId");
			bean.setEntityId(input.getLongValue());

			JsonUtils.nextExpect(input, "typeName");
			String type = input.getValueAsString();
			BaseBean idType = mapper.getIdentityTypeByName(type);
			bean.setTypeId(idType.getId());
			
			JsonUtils.nextExpect(input, JsonToken.END_OBJECT);
			
			IdentityBean toAdd = bean;
			if (header.getVersionMajor() == 1 && header.getVersionMinor() < 1)
				toAdd = update0To1(toAdd, type);
			if (toAdd == null)
				continue;
			
			if (header.getVersionMajor() == 1 && header.getVersionMinor() < 3)
				toAdd = update2To3(toAdd, type);
			if (toAdd == null)
				continue;
			
			Identity identity = idResolver.resolveIdentityBeanNoExternalize(toAdd, mapper);
			dbIdentities.insertIdentity(identity, toAdd.getEntityId(), true, sql);
		}
		JsonUtils.expect(input, JsonToken.END_ARRAY);
	}
	
	private void serializeToJson(JsonGenerator jg, IdentityBean bean, IdentitiesMapper mapper) 
			throws JsonGenerationException, IOException, IllegalTypeException
	{
		super.serializeBaseBeanToJson(jg, bean);
		jg.writeNumberField("entityId", bean.getEntityId());
		Identity id = idResolver.resolveIdentityBeanNoExternalize(bean, mapper);
		jg.writeStringField("typeName", id.getTypeId());
	}
	
	/**
	 * Performs an update of identity in pre 1.1 format. As multiple identities may be created a list is returned.
	 * Algorithm:
	 * <ul>
	 * <li> transient identities and targeted persistent are dropped
	 * <li> persistent not targeted have a 'value' element added in their contents 
	 * <li> all other are unchanged 
	 * </ul>
	 * 
	 * @param input
	 * @return
	 * @throws IllegalTypeException 
	 */
	private IdentityBean update0To1(IdentityBean input, String typeName) throws IllegalTypeException
	{
		if (TransientIdentity.ID.equals(typeName) || TargetedPersistentIdentity.ID.equals(typeName))
			return null;
		
		final String PFX = "persistent::"; 
		if (PersistentIdentity.ID.equals(typeName))
		{
			try
			{
				byte[] cont = input.getContents();
				ObjectMapper jsonMapper = new ObjectMapper();
				ObjectNode node = (ObjectNode) jsonMapper.readTree(cont);
				String name = input.getName();
				if (name.startsWith(PFX))
					name = name.substring(PFX.length());
				node.put("value", name);
				node.remove("target");
				node.remove("realm");
				byte[] cont2 = jsonMapper.writeValueAsBytes(node);
				input.setContents(cont2);
			} catch (Exception e)
			{
				throw new IllegalStateException("Bug: the JSON generated by the system is invalid");
			}
		}

		return input;
	}
	
	/**
	 * All names are hashed. Transient identities are dropped.
	 * @param input
	 * @param typeName
	 * @return
	 * @throws IllegalTypeException
	 */
	private IdentityBean update2To3(IdentityBean input, String typeName) throws IllegalTypeException
	{
		String originalName = input.getName();
		if (TransientIdentity.ID.equals(typeName))
			return null;

		input.setName(IdentitiesResolver.hashIdentity(originalName));
		return input;
	}
}
