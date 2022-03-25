/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_9;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypeBean;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypesMapper;
import pl.edu.icm.unity.store.migration.InDBContentsUpdater;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;
import pl.edu.icm.unity.types.basic.AttributeType;


@Component
public class InDBUpdateFromSchema15 implements InDBContentsUpdater
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_DB, InDBUpdateFromSchema15.class);
	
	@Override
	public int getUpdatedVersion()
	{
		return 15;
	}
	
	@Override
	public void update() throws IOException
	{
		updateStringSyntaxAttributeType();
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
