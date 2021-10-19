/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.idpStatistic;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.IdpStatisticManagement;
import pl.edu.icm.unity.engine.api.IdpStatisticManagement.GroupBy;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic.Status;

public class IdpStatisticsManagementTest extends DBIntegrationTestBase
{
	@Autowired
	private IdpStatisticManagement statMan;

	@Before
	@Override
	public void setupAdmin() throws Exception
	{
		super.setupAdmin();
		InvocationContext invContext = InvocationContext.getCurrent();
		invContext.setLoginSession(new LoginSession("1", null, null, 100, 1L, null, null, null, null));
		statMan.deleteOlderThan(LocalDateTime.now());
	}

	@Test
	public void shouldReturnAddedStatistic() throws EngineException, JsonProcessingException
	{
		IdpStatistic s1 = IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("eName").clientId("c")
				.clientName("cName").timestamp(LocalDateTime.now()).status(Status.SUCCESSFUL).build();
		statMan.addIdpStatistic(s1);
		List<IdpStatistic> idpStatisticsSince = statMan.getIdpStatisticsSince(LocalDateTime.now().minusDays(10), 1000);
		assertThat(idpStatisticsSince.size(), is(1));
		assertThat(idpStatisticsSince.get(0), is(s1));
	}

	@Test
	public void shouldReturnStatisticsSinceGivenDate() throws EngineException, JsonProcessingException
	{
		IdpStatistic s1 = IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("eName").clientId("c")
				.clientName("cName").timestamp(LocalDateTime.now()).status(Status.SUCCESSFUL).build();

		IdpStatistic s2 = IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("eName").clientId("c")
				.clientName("cName").timestamp(LocalDateTime.now().minusDays(2)).status(Status.SUCCESSFUL).build();

		IdpStatistic s3 = IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("eName").clientId("c")
				.clientName("cName").timestamp(LocalDateTime.now().minusDays(3)).status(Status.SUCCESSFUL).build();

		IdpStatistic s4 = IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("eName").clientId("c")
				.clientName("cName").timestamp(LocalDateTime.now().minusDays(4)).status(Status.SUCCESSFUL).build();

		statMan.addIdpStatistic(s1);
		statMan.addIdpStatistic(s2);
		statMan.addIdpStatistic(s3);
		statMan.addIdpStatistic(s4);
		List<IdpStatistic> idpStatisticsSince = statMan.getIdpStatisticsSince(LocalDateTime.now().minusDays(3), 1000);
		assertThat(idpStatisticsSince.size(), is(2));
		assertThat(idpStatisticsSince.contains(s1), is(true));
		assertThat(idpStatisticsSince.contains(s2), is(true));
	}

	@Test
	public void shouldReturnGroupedStatistics() throws EngineException, JsonProcessingException
	{
		IdpStatistic s1 = IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("eName").clientId("c")
				.clientName("cName").timestamp(LocalDateTime.now()).status(Status.SUCCESSFUL).build();

		IdpStatistic s2 = IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("eName").clientId("c")
				.clientName("cName").timestamp(LocalDateTime.now().minusDays(2)).status(Status.SUCCESSFUL).build();

		IdpStatistic s3 = IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("eName").clientId("c")
				.clientName("cName").timestamp(LocalDateTime.now().minusDays(3)).status(Status.SUCCESSFUL).build();

		IdpStatistic s4 = IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("eName").clientId("c")
				.clientName("cName").timestamp(LocalDateTime.now().minusDays(4)).status(Status.FAILED).build();

		statMan.addIdpStatistic(s1);
		statMan.addIdpStatistic(s2);
		statMan.addIdpStatistic(s3);
		statMan.addIdpStatistic(s4);
		List<GroupedIdpStatistic> idpStatisticsSince = statMan
				.getIdpStatisticsSinceGroupBy(LocalDateTime.now().minusDays(10), GroupBy.total, 1000);
		assertThat(idpStatisticsSince.size(), is(1));
		assertThat(idpStatisticsSince.get(0).idpId, is("eid"));
		assertThat(idpStatisticsSince.get(0).clientId, is("c"));
		assertThat(idpStatisticsSince.get(0).sigInStats.get(0).failedCount, is(1L));
		assertThat(idpStatisticsSince.get(0).sigInStats.get(0).successfullCount, is(3L));
	}

	@Test
	public void shouldDropOlderThan() throws EngineException, JsonProcessingException
	{
		IdpStatistic s1 = IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("eName").clientId("c")
				.clientName("cName").timestamp(LocalDateTime.now()).status(Status.SUCCESSFUL).build();

		IdpStatistic s2 = IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("eName").clientId("c")
				.clientName("cName").timestamp(LocalDateTime.now().minusDays(2)).status(Status.SUCCESSFUL).build();

		IdpStatistic s3 = IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("eName").clientId("c")
				.clientName("cName").timestamp(LocalDateTime.now().minusDays(3)).status(Status.SUCCESSFUL).build();

		IdpStatistic s4 = IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("eName").clientId("c")
				.clientName("cName").timestamp(LocalDateTime.now().minusDays(4)).status(Status.SUCCESSFUL).build();

		statMan.addIdpStatistic(s1);
		statMan.addIdpStatistic(s2);
		statMan.addIdpStatistic(s3);
		statMan.addIdpStatistic(s4);

		statMan.deleteOlderThan(LocalDateTime.now().minusDays(1));

		List<IdpStatistic> idpStatisticsSince = statMan.getIdpStatisticsSince(LocalDateTime.now().minusDays(10), 1000);
		assertThat(idpStatisticsSince.size(), is(1));
		assertThat(idpStatisticsSince.get(0), is(s1));
	}

}
