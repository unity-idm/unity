/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_8;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypeBean;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypesMapper;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.migration.InDBContentsUpdater;
import pl.edu.icm.unity.store.objstore.endpoint.EndpointHandler;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;


@Component
public class InDBUpdateFromSchema14 implements InDBContentsUpdater
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_DB, InDBUpdateFromSchema14.class);
	
	@Autowired
	private ObjectStoreDAO genericObjectsDAO;
	
	@Override
	public int getUpdatedVersion()
	{
		return 14;
	}
	
	@Override
	public void update() throws IOException
	{
		updateStringSyntaxAttributeType();
		updateOAuthEndpointConfiguration();
	}

	private void updateOAuthEndpointConfiguration()
	{
		List<GenericObjectBean> endpoints = genericObjectsDAO.getObjectsOfType(EndpointHandler.ENDPOINT_OBJECT_TYPE);
		for (GenericObjectBean endpoint : endpoints)
		{
			ObjectNode parsed = JsonUtil.parse(endpoint.getContents());
			String typeId = parsed.get("typeId").asText();	
			if ("OAuth2Authz".equals(typeId) || "OAuth2Token".equals(typeId))
			{
				ObjectNode configuration = (ObjectNode) parsed.get("configuration");
				JsonNode iconfiguration = configuration.get("configuration");
				JsonNode migratedConfig = new OAuthEndpointConfigurationMigrator(iconfiguration).migrate();
				configuration.set("configuration", migratedConfig);
				
				endpoint.setContents(JsonUtil.serialize2Bytes(parsed));
				LOG.info("Updating OAuth endpoint {} with id {}, \nold config: {}\nnew config: {}", 
						endpoint.getName(), endpoint.getId(), configuration, migratedConfig);
				genericObjectsDAO.updateByKey(endpoint.getId(), endpoint);
			}
		}		
	}
	
	private void updateStringSyntaxAttributeType()
	{
		AttributeTypesMapper atTypeMapper = SQLTransactionTL.getSql().getMapper(AttributeTypesMapper.class);
		List<AttributeTypeBean> atTypes = atTypeMapper.getAll();
		for (AttributeTypeBean atType : atTypes)
		{
			if ("string".equals(atType.getValueSyntaxId()))
			{
				AttributeType at = new AttributeType();
				at.setName(atType.getName());
				at.setValueSyntax(atType.getValueSyntaxId());
				at.fromJsonBase(JsonUtil.parse(atType.getContents()));
				JsonNode valueSyntaxConfigurationOrg = at.getValueSyntaxConfiguration();
				if (valueSyntaxConfigurationOrg == null || valueSyntaxConfigurationOrg.isNull())
					continue;

				ObjectNode newValueSyntaxConfiguration = (ObjectNode) valueSyntaxConfigurationOrg;
				if (newValueSyntaxConfiguration.get("maxLength").asInt() > 1000)
				{
					newValueSyntaxConfiguration.put("editWithTextArea", "true");
				} else
				{
					newValueSyntaxConfiguration.put("editWithTextArea", "false");
				}
				at.setValueSyntaxConfiguration(newValueSyntaxConfiguration);
				atType.setContents(JsonUtil.serialize2Bytes(at.toJsonBase()));

				LOG.info("Updating attribute type {}, set editWithTextArea={} in string syntax", at.getName(),
						newValueSyntaxConfiguration.get("editWithTextArea"));
				atTypeMapper.updateByKey(atType);
			}
		}
	}
}
