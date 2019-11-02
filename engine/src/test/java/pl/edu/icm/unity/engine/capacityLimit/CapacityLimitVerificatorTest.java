/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.capacityLimit;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.CapacityLimitManagement;
import pl.edu.icm.unity.engine.capacityLimits.InternalCapacityLimitVerificator;
import pl.edu.icm.unity.exceptions.CapacityLimitReachedException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.capacityLimit.CapacityLimit;
import pl.edu.icm.unity.types.capacityLimit.CapacityLimitName;

public class CapacityLimitVerificatorTest extends DBIntegrationTestBase
{
	@Autowired
	private CapacityLimitManagement capacityMan;

	@Autowired
	private InternalCapacityLimitVerificator capacityLimit;

	@Autowired
	private TransactionalRunner txRunner;

	@Test(expected = CapacityLimitReachedException.class)
	public void shouldThrowLimitExceededException() throws EngineException
	{
		capacityMan.setLimit(new CapacityLimit(CapacityLimitName.Groups, 1));
		txRunner.runInTransactionThrowing(() -> {
			capacityLimit.assertInSystemLimitForSingleAdd(CapacityLimitName.Groups, 2);
		});
	}
}