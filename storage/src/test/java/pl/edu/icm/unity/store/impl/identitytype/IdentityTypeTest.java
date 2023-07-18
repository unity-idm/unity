/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identitytype;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.identity.IdentityType;
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

			assertThat(all).isNotNull();

			assertThat(all).hasSize(1);

			IdentityType fromList = all.get(0);

			assertThat(fromList).isEqualTo(obj);
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

			assertThat(asMap).isNotNull();

			assertThat(asMap).hasSize(1);

			assertThat(asMap.keySet()).contains(obj.getName());

			assertThat(asMap.get(obj.getName())).isEqualTo(obj);
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
