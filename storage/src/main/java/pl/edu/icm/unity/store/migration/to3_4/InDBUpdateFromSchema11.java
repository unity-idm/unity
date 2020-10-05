/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_4;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.impl.attribute.AttributeBean;
import pl.edu.icm.unity.store.impl.attribute.AttributesMapper;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypeBean;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypesMapper;
import pl.edu.icm.unity.store.migration.InDBContentsUpdater;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;

/**
 * changes the legacy jpegImage to a properly implemented image
 */
@Component
public class InDBUpdateFromSchema11 implements InDBContentsUpdater
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, InDBUpdateFromSchema11.class);
	
	@Override
	public int getUpdatedVersion()
	{
		return 11;
	}
	
	@Override
	public void update() throws IOException
	{
		updateAttributes();
		updateAttributeTypes();
	}

	private void updateAttributes()
	{
		AttributesMapper attrMapper = SQLTransactionTL.getSql().getMapper(AttributesMapper.class);
		List<AttributeBean> all = attrMapper.getAll();
		for (AttributeBean attributeBean : all)
		{
			if ("jpegImage".equals(attributeBean.getValueSyntaxId()))
			{
				log.info("Converting attribute {} (of {} in {}) to image syntax type", attributeBean.getName(),
						attributeBean.getEntityId(), attributeBean.getGroup());
				byte[] values = attributeBean.getValues();
				ObjectNode parsed = JsonUtil.parse(values);
				ArrayNode valuesArray = parsed.withArray("values");
				UpdateHelperTo12.updateValuesJson(valuesArray);
				attributeBean.setValues(JsonUtil.serialize2Bytes(parsed));
				attrMapper.updateByKey(attributeBean);
			}
		}
	}

	private void updateAttributeTypes()
	{
		AttributeTypesMapper atTypeMapper = SQLTransactionTL.getSql().getMapper(AttributeTypesMapper.class);
		List<AttributeTypeBean> atTypes = atTypeMapper.getAll();
		for (AttributeTypeBean atType: atTypes)
		{
			if ("jpegImage".equals(atType.getValueSyntaxId()))
			{
				log.info("Converting attribute type {} to use image syntax type", atType.getName());
				atType.setValueSyntaxId("image");
				atTypeMapper.updateByKey(atType);
			}
		}
	}
}
