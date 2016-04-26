/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identitytype;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.store.AbstractNamedDAOTest;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.api.NamedCRUDDAO;
import pl.edu.icm.unity.store.mocks.MockIdentityTypeDef;
import pl.edu.icm.unity.types.basic.IdentityType;

public class IdentityTypeTest extends AbstractNamedDAOTest<IdentityType>
{
	@Autowired
	private IdentityTypeDAO idTypeDAO;

	@Override
	protected NamedCRUDDAO<IdentityType> getDAO()
	{
		return idTypeDAO;
	}

	@Override
	protected IdentityType getObject(String name)
	{
		IdentityType idType = new IdentityType(new MockIdentityTypeDef());
		idType.setDescription("d");
		idType.setMaxInstances(10);
		idType.setMinInstances(0);
		idType.setSelfModificable(true);
		idType.getExtractedAttributes().put("a", "b");
		idType.getExtractedAttributes().put("aa", "bb");
		return idType;
	}

	@Override
	protected void mutateObject(IdentityType src)
	{
		src.setDescription("d2");
		src.setMaxInstances(20);
		src.setMinInstances(1);
		src.setSelfModificable(false);
		src.getExtractedAttributes().clear();
		src.getExtractedAttributes().put("c", "g");
		src.getExtractedAttributes().put("cc", "gg");
	}

	@Override
	protected void assertAreEqual(IdentityType obj, IdentityType cmp)
	{
		assertThat(obj.getIdentityTypeProvider().getId(), is(cmp.getIdentityTypeProvider().getId()));
		assertThat(obj.getDescription(), is (cmp.getDescription()));
		assertThat(obj.getMaxInstances(), is (cmp.getMaxInstances()));
		assertThat(obj.getMinInstances(), is (cmp.getMinInstances()));
		assertThat(obj.isSelfModificable(), is(cmp.isSelfModificable()));
		assertThat(obj.getExtractedAttributes(), is(cmp.getExtractedAttributes()));
	}

	@Test
	@Override
	public void shouldReturnTwoCreatedWithinCollections()
	{
		tx.runInTransaction(() -> {
			IdentityType obj = getObject("");
			idTypeDAO.create(obj);

			List<IdentityType> all = idTypeDAO.getAll();

			assertThat(all, is(notNullValue()));

			assertThat(all.size(), is(1));

			IdentityType fromList = all.get(0);

			assertAreEqual(fromList, obj);
		});
	}
	
	@Test
	@Override
	public void shouldReturnTwoCreatedWithinCollectionsByName()
	{
		tx.runInTransaction(() -> {
			IdentityType obj = getObject("");
			idTypeDAO.create(obj);

			Map<String, IdentityType> asMap = idTypeDAO.getAsMap();

			assertThat(asMap, is(notNullValue()));

			assertThat(asMap.size(), is(1));

			assertThat(asMap.containsKey(obj.getName()), is(true));

			assertAreEqual(asMap.get(obj.getName()), obj);
		});
	}
}
