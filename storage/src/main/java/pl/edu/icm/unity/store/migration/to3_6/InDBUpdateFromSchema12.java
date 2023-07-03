/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_6;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.impl.objstore.GenericMapper;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.migration.InDBContentsUpdater;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;

/**
 * 1. update scopes configuration in oauth-rp authenticator
 */
@Component
public class InDBUpdateFromSchema12 implements InDBContentsUpdater
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_DB, InDBUpdateFromSchema12.class);
	
	@Override
	public int getUpdatedVersion()
	{
		return 12;
	}
	
	@Override
	public void update() throws IOException
	{
		updateScopesConfigInOauthRp();
	}

	private void updateScopesConfigInOauthRp()
	{
		GenericMapper genericMapper = SQLTransactionTL.getSql().getMapper(GenericMapper.class);
		List<GenericObjectBean> authenticators = genericMapper.selectObjectsByType("authenticator");
		for (GenericObjectBean authenticator: authenticators)
		{
			ObjectNode parsed = JsonUtil.parse(authenticator.getContents());
			if ("oauth-rp".equals(parsed.get("verificationMethod").asText()))
			{
				JsonNode configuration = parsed.get("configuration");
				JsonNode migratedConfig = new OauthRpConfigurationMigrator(configuration).migrate();
				parsed.set("configuration", migratedConfig);
				
				authenticator.setContents(JsonUtil.serialize2Bytes(parsed));
				LOG.info("Updating authenticator {} with id {}, \nold config: {}\nnew config: {}", 
						authenticator.getName(), authenticator.getId(), configuration, migratedConfig);
				genericMapper.updateByKey(authenticator);
			}
		}
	}
	
}
