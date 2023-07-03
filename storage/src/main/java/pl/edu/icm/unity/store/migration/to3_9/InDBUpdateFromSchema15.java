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

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypeBean;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypesMapper;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.migration.InDBContentsUpdater;
import pl.edu.icm.unity.store.objstore.reg.form.RegistrationFormHandler;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;


@Component
public class InDBUpdateFromSchema15 implements InDBContentsUpdater
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_DB, InDBUpdateFromSchema15.class);
	
	private final ObjectStoreDAO genericObjectsDAO;
	
	public InDBUpdateFromSchema15(ObjectStoreDAO genericObjectsDAO)
	{
		this.genericObjectsDAO = genericObjectsDAO;
	}

	@Override
	public int getUpdatedVersion()
	{
		return 15;
	}
	
	@Override
	public void update() throws IOException
	{
		updateStringSyntaxAttributeType();
		migrateExternalSignupSpecInRegistrationForms();
	}
	
	private void migrateExternalSignupSpecInRegistrationForms()
	{
		List<GenericObjectBean> forms = genericObjectsDAO.getObjectsOfType(RegistrationFormHandler.REGISTRATION_FORM_OBJECT_TYPE);
		for (GenericObjectBean form : forms)
		{
			ObjectNode objContent = JsonUtil.parse(form.getContents());
			UpdateHelperTo3_9.migrateExternalSignupSpec(objContent).ifPresent(updatedContent ->
			{
				form.setContents(JsonUtil.serialize2Bytes(updatedContent));
				LOG.info("Updating registration form: {}, previous value: {}, new value: {}", 
						form.getName(), objContent.toString(), updatedContent.toString());
				genericObjectsDAO.updateByKey(form.getId(), form);
			});
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
