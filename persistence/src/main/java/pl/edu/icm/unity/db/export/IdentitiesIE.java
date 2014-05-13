/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.export;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
			
			List<IdentityBean> toAdd;
			if (header.getVersionMajor() == 1 && header.getVersionMinor() < 1)
				toAdd = update0To1(bean, type);
			else
				toAdd = Collections.singletonList(bean);
			
			for (IdentityBean beanI: toAdd)
			{
				Identity identity = idResolver.resolveIdentityBeanNoExternalize(beanI, mapper);
				dbIdentities.insertIdentity(identity, beanI.getEntityId(), true, sql);
			}
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
	private List<IdentityBean> update0To1(IdentityBean input, String typeName) throws IllegalTypeException
	{
		List<IdentityBean> ret = new ArrayList<>();
		if (TransientIdentity.ID.equals(typeName) || TargetedPersistentIdentity.ID.equals(typeName))
			return ret;
		
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
				byte[] cont2 = jsonMapper.writeValueAsBytes(node);
				input.setContents(cont2);
				ret.add(input);
				return ret;
			} catch (Exception e)
			{
				throw new IllegalStateException("Bug: the JSON generated by the system is invalid");
			}
		}
		ret.add(input);
		return ret;
	}
}
