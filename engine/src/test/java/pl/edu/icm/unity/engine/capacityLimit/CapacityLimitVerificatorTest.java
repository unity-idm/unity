/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.capacityLimit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.capacity_limit.CapacityLimit;
import pl.edu.icm.unity.base.capacity_limit.CapacityLimitName;
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

	@Test
	public void shouldThrowLimitExceededException() throws EngineException
	{
		txRunner.runInTransactionThrowing(() ->
		{
			limitDB.create(new CapacityLimit(CapacityLimitName.GroupsCount, 1));
		});

		Assertions.assertThrows(CapacityLimitReachedException.class, () -> txRunner.runInTransactionThrowing(() ->
		{
			capacityLimitVerificator.assertInSystemLimitForSingleAdd(CapacityLimitName.GroupsCount, () -> 2L);
		}));
	}
}
