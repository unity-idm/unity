/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.base.internal.TransactionalRunner;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml"})
public class AttributeTypeTest
{
	@Autowired
	private AttributeTypeDAO attrTypeDAO;
	
	@Autowired
	private TransactionalRunner tx;
	
	@Test
	public void shouldReturnCreatedAttributeType() throws EngineException
	{
		tx.runInTransaction(() -> {
			if (attrTypeDAO.exists("attribute1"))
				attrTypeDAO.delete("attribute1");
		});

		boolean doesntExist = tx.runInTransactionRet(() -> {
			return attrTypeDAO.exists("attribute1");
		});
		assertThat(doesntExist, is(false));
		
		
		AttributeType created = new AttributeType("attribute1", new MockAttributeSyntax());
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
		
		tx.runInTransaction(() -> {
			attrTypeDAO.create(created);
		});
		
		
		Map<String, AttributeType> readMap = tx.runInTransactionRet(() -> {
			return attrTypeDAO.getAsMap();
		});
		
		assertThat(readMap.size(), is(1));
		AttributeType read = readMap.get("attribute1");
		assertThat(read.getDisplayedName(), is(new I18nString("Attribute 1")));
		assertThat(read.getDescription(), is("desc"));
		assertThat(read.getName(), is("attribute1"));
		assertThat(read.getFlags(), is(8));
		assertThat(read.getMaxElements(), is(10));
		assertThat(read.getMinElements(), is(1));
		assertThat(read.getValueType().getValueSyntaxId(), is(MockAttributeSyntax.ID));
		assertThat(read.getVisibility(), is(AttributeVisibility.local));
		assertThat(read.getMetadata().get("1"), is("a"));
	}
}
