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

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.utils.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.impl.attribute.AttributeBean;
import pl.edu.icm.unity.store.impl.attribute.AttributesMapper;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypeBean;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypesMapper;
import pl.edu.icm.unity.store.impl.objstore.GenericMapper;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.migration.InDBContentsUpdater;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;

/**
 * 1. changes the legacy jpegImage to a properly implemented image
 * 2. changes the project management role attribute
 */
@Component
public class InDBUpdateFromSchema11 implements InDBContentsUpdater
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, InDBUpdateFromSchema11.class);
	
	@Override
	public int getUpdatedVersion()
	{
		return 11;
	}
	
	@Override
	public void update() throws IOException
	{
		updateJpegAttributes();
		updateJpegAttributeTypes();
		updateProjectManagementRoleAttributeType();
		dropAdminUIEndpoint();
	}

	private void dropAdminUIEndpoint()
	{
		GenericMapper genericMapper = SQLTransactionTL.getSql().getMapper(GenericMapper.class);
		List<GenericObjectBean> endpoints = genericMapper.selectObjectsByType("endpointDefinition");
		for (GenericObjectBean endpoint: endpoints)
		{
			ObjectNode parsed = JsonUtil.parse(endpoint.getContents());
			if ("WebAdminUI".equals(parsed.get("typeId").asText()))
			{
				log.info("Dropping AdminUI endpoint {} with id {}", endpoint.getName(), endpoint.getId());
				genericMapper.deleteByKey(endpoint.getId());
			}
		}
	}

	private void updateJpegAttributes()
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

	private void updateJpegAttributeTypes()
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
	
	private void updateProjectManagementRoleAttributeType()
	{
		AttributeTypesMapper atTypeMapper = SQLTransactionTL.getSql().getMapper(AttributeTypesMapper.class);
		List<AttributeTypeBean> atTypes = atTypeMapper.getAll();
		for (AttributeTypeBean atType : atTypes)
		{
			if ("sys:ProjectManagementRole".equals(atType.getName()))
			{
				log.info("Updating attribute type {} adding new value projectsAdmin", 
						atType.getName());
				AttributeType at = new AttributeType();
				at.setName(atType.getName());
				at.setValueSyntax(atType.getValueSyntaxId());
				at.fromJsonBase(JsonUtil.parse(atType.getContents()));
				at.setValueSyntaxConfiguration(UpdateHelperTo12.getProjectRoleAttributeSyntaxConfig());
				if (at.getDescription() == null || at.getDescription().isEmpty())
				{
					at.setDescription(UpdateHelperTo12.getProjectRoleDescription());
				}
				atType.setContents(JsonUtil.serialize2Bytes(at.toJsonBase()));
				atTypeMapper.updateByKey(atType);
			}
		}
	}
}
