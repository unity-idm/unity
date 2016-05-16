/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attributetype;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.NamedCRUDDAO;
import pl.edu.icm.unity.store.impl.AbstractNamedDAOTest;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;

public class AttributeTypeTest extends AbstractNamedDAOTest<AttributeType>
{
	@Autowired
	private AttributeTypeDAO attrTypeDAO;
	
	@Override
	protected NamedCRUDDAO<AttributeType> getDAO()
	{
		return attrTypeDAO;
	}

	@Test
	public void importExportIsIdempotent()
	{
		super.importExportIsIdempotent();
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
		created.setValueSyntaxConfiguration("some config");
		return created;
	}

	@Override
	protected void mutateObject(AttributeType src)
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
	}

	@Override
	protected void assertAreEqual(AttributeType obj, AttributeType cmp)
	{
		assertThat(obj, is(cmp));
	}
}
