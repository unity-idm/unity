/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attributetype;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.NamedCRUDDAO;
import pl.edu.icm.unity.store.impl.AbstractNamedDAOTest;

public class AttributeTypeTest extends AbstractNamedDAOTest<AttributeType>
{
	@Autowired
	private AttributeTypeDAO attrTypeDAO;
	
	@Override
	protected NamedCRUDDAO<AttributeType> getDAO()
	{
		return attrTypeDAO;
	}

	@Override
	protected AttributeType getObject(String name)
	{
		AttributeType created = new AttributeType(name, "syntax");
		created.setDescription(new I18nString("desc"));
		created.setDisplayedName(new I18nString("Attribute 1"));
		created.setFlags(8);
		created.setUniqueValues(true);
		created.setMaxElements(10);
		created.setMinElements(1);
		created.setSelfModificable(true);
		Map<String, String> meta = new HashMap<>();
		meta.put("1", "a");
		created.setMetadata(meta);
		ObjectNode cfg = Constants.MAPPER.createObjectNode();
		cfg.put("test", "testV");
		created.setValueSyntaxConfiguration(cfg);
		return created;
	}

	@Override
	protected AttributeType mutateObject(AttributeType src)
	{
		src.setDescription(new I18nString("desc2"));
		src.setDisplayedName(new I18nString("Attribute 1 updated"));
		src.setFlags(4);
		src.setUniqueValues(false);
		src.setMaxElements(4);
		src.setMinElements(2);
		src.setSelfModificable(false);
		Map<String, String> meta = new HashMap<>();
		meta.put("2", "b");
		src.setValueSyntaxConfiguration(null);
		src.setMetadata(meta);
		return src;
	}
}
