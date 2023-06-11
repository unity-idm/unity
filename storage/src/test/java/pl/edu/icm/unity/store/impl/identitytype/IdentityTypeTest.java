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

import pl.edu.icm.unity.base.entity.IdentityType;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.api.NamedCRUDDAO;
import pl.edu.icm.unity.store.impl.AbstractNamedDAOTest;

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
		IdentityType idType = new IdentityType(name);
		idType.setIdentityTypeProvider("identityTypeProvider");
		idType.setIdentityTypeProviderSettings("{}");
		idType.setDescription("d");
		idType.setMaxInstances(10);
		idType.setMinInstances(0);
		idType.setSelfModificable(true);
		return idType;
	}

	@Override
	protected IdentityType mutateObject(IdentityType src)
	{
		src.setIdentityTypeProviderSettings("{ccc=1}");
		src.setDescription("d2");
		src.setMaxInstances(20);
		src.setMinInstances(1);
		src.setSelfModificable(false);
		return src;
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

			assertThat(fromList, is(obj));
		});
	}
	
	@Test
	@Override
	public void shouldReturnTwoCreatedWithinCollectionsByName()
	{
		tx.runInTransaction(() -> {
			IdentityType obj = getObject("");
			idTypeDAO.create(obj);

			Map<String, IdentityType> asMap = idTypeDAO.getAllAsMap();

			assertThat(asMap, is(notNullValue()));

			assertThat(asMap.size(), is(1));

			assertThat(asMap.containsKey(obj.getName()), is(true));

			assertThat(asMap.get(obj.getName()), is(obj));
		});
	}
	
	@Test
	public void shouldFailOnCreatingWithTooLongName()
	{
		//nop
	}

	@Test
	public void shouldFailOnUpdatingToTooLongName()
	{
		//nop
	}
}
