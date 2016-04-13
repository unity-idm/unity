/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.internal.TransactionalRunner;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.types.basic.IdentityType;

public class IdentityTypeTest extends AbstractDAOTest
{
	@Autowired
	private IdentityTypeDAO idTypeDAO;
	
	@Autowired
	private TransactionalRunner tx;
	
	@Test
	public void shouldReturnCreatedIdentityType() throws EngineException
	{
		IdentityType idType = new IdentityType(new MockIdentityTypeDef());
		idType.setDescription("d");
		idType.setMaxInstances(10);
		idType.setMinInstances(0);
		idType.setSelfModificable(true);
		idType.getExtractedAttributes().put("a", "b");
		idType.getExtractedAttributes().put("aa", "bb");
		
		tx.runInTransaction(() -> {
			idTypeDAO.create(idType);
		});
		
		
		Map<String, IdentityType> identityTypes = tx.runInTransactionRet(() -> {
			return idTypeDAO.getAsMap();
		});
		
		assertThat(identityTypes.size(), is(1));
		IdentityType identityType = identityTypes.get(MockIdentityTypeDef.NAME);
		assertThat(identityType.getDescription(), is ("d"));
		assertThat(identityType.getMaxInstances(), is (10));
		assertThat(identityType.getMinInstances(), is (0));
		assertThat(identityType.isSelfModificable(), is(true));
		assertThat(identityType.getExtractedAttributes().get("a"), is("b"));
		assertThat(identityType.getExtractedAttributes().get("aa"), is("bb"));
	}
}
