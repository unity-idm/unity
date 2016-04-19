/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attributetype;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.store.AbstractDAOTest;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.BasicCRUDDAO;
import pl.edu.icm.unity.store.mocks.MockAttributeSyntax;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

public class AttributeTypeTest extends AbstractDAOTest<AttributeType>
{
	@Autowired
	private AttributeTypeDAO attrTypeDAO;
	
	@Override
	protected BasicCRUDDAO<AttributeType> getDAO()
	{
		return attrTypeDAO;
	}

	@Override
	protected AttributeType getObject(String name)
	{
		AttributeType created = new AttributeType(name, new MockAttributeSyntax());
		created.setDescription(new I18nString("desc"));
		created.setDisplayedName(new I18nString("Attribute 1"));
		created.setFlags(8);
		created.setUniqueValues(true);
		created.setVisibility(AttributeVisibility.local);
		created.setMaxElements(10);
		created.setMinElements(1);
		created.setSelfModificable(true);
		Map<String, String> meta = new HashMap<>();
		meta.put("1", "a");
		created.setMetadata(meta);
		return created;
	}

	@Override
	protected void mutateObject(AttributeType src)
	{
		src.setDescription(new I18nString("desc2"));
		src.setDisplayedName(new I18nString("Attribute 1 updated"));
		src.setFlags(4);
		src.setUniqueValues(false);
		src.setVisibility(AttributeVisibility.full);
		src.setMaxElements(4);
		src.setMinElements(2);
		src.setSelfModificable(false);
		Map<String, String> meta = new HashMap<>();
		meta.put("2", "b");
		src.setMetadata(meta);
	}

	@Override
	protected String getName(AttributeType obj)
	{
		return obj.getName();
	}

	@Override
	protected void assertAreEqual(AttributeType obj, AttributeType cmp)
	{
		assertThat(obj.getName(), is(cmp.getName()));
		assertThat(obj.getDisplayedName(), is(cmp.getDisplayedName()));
		assertThat(obj.getDescription(), is(cmp.getDescription()));
		assertThat(obj.getFlags(), is(cmp.getFlags()));
		assertThat(obj.getMaxElements(), is(cmp.getMaxElements()));
		assertThat(obj.getMinElements(), is(cmp.getMinElements()));
		assertThat(obj.getValueType().getValueSyntaxId(), is(cmp.getValueType().getValueSyntaxId()));
		assertThat(obj.getVisibility(), is(cmp.getVisibility()));
		assertThat(obj.getMetadata(), is(cmp.getMetadata()));
	}
}
