/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.capacityLimit;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.capacityLimit.CapacityLimit;
import pl.edu.icm.unity.base.capacityLimit.CapacityLimitName;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.exceptions.CapacityLimitReachedException;
import pl.edu.icm.unity.engine.capacityLimits.InternalCapacityLimitVerificator;
import pl.edu.icm.unity.store.api.generic.CapacityLimitDB;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

public class CapacityLimitVerificatorTest extends DBIntegrationTestBase
{
	@Autowired
	private CapacityLimitDB limitDB;

	@Autowired
	private InternalCapacityLimitVerificator capacityLimitVerificator;

	@Autowired
	private TransactionalRunner txRunner;

	@Test(expected = CapacityLimitReachedException.class)
	public void shouldThrowLimitExceededException() throws EngineException
	{
		txRunner.runInTransactionThrowing(() -> {
			limitDB.create(new CapacityLimit(CapacityLimitName.GroupsCount, 1));
		});
		txRunner.runInTransactionThrowing(() -> {
			capacityLimitVerificator.assertInSystemLimitForSingleAdd(CapacityLimitName.GroupsCount, () -> 2L);
		});
	}
}
