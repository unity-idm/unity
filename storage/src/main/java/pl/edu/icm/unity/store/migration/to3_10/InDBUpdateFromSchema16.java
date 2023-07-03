/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_10;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.migration.InDBContentsUpdater;
import pl.edu.icm.unity.store.objstore.endpoint.EndpointHandler;

@Component
public class InDBUpdateFromSchema16 implements InDBContentsUpdater
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_DB, InDBUpdateFromSchema16.class);

	private final ObjectStoreDAO genericObjectsDAO;

	public InDBUpdateFromSchema16(ObjectStoreDAO genericObjectsDAO)
	{
		this.genericObjectsDAO = genericObjectsDAO;
	}

	@Override
	public int getUpdatedVersion()
	{
		return 16;
	}

	@Override
	public void update() throws IOException
	{
		updateHomeEndpointConfiguration();
	}

	private void updateHomeEndpointConfiguration()
	{
		List<GenericObjectBean> endpoints = genericObjectsDAO.getObjectsOfType(EndpointHandler.ENDPOINT_OBJECT_TYPE);
		for (GenericObjectBean endpoint : endpoints)
		{
			ObjectNode parsed = JsonUtil.parse(endpoint.getContents());
			String typeId = parsed.get("typeId").asText();
			if ("UserHomeUI".equals(typeId))
			{
				ObjectNode configuration = (ObjectNode) parsed.get("configuration");
				JsonNode iconfiguration = configuration.get("configuration");
				JsonNode migratedConfig = new HomeEndpointConfigurationMigrator(iconfiguration).migrate();
				configuration.set("configuration", migratedConfig);
				endpoint.setContents(JsonUtil.serialize2Bytes(parsed));
				LOG.info("Updating HomeUI endpoint {} with id {}, \nold config: {}\nnew config: {}", endpoint.getName(),
						endpoint.getId(), configuration, migratedConfig);
				genericObjectsDAO.updateByKey(endpoint.getId(), endpoint);
			}
		}
	}
}
