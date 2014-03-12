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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.mapper.IdentitiesMapper;
import pl.edu.icm.unity.db.model.BaseBean;
import pl.edu.icm.unity.db.model.IdentityBean;
import pl.edu.icm.unity.db.resolvers.IdentitiesResolver;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
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
	
	public void deserialize(SqlSession sql, JsonParser input) throws IOException, EngineException
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
			
			Identity identity = idResolver.resolveIdentityBeanNoExternalize(bean, mapper);
			dbIdentities.insertIdentity(identity, bean.getEntityId(), true, sql);
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
}
