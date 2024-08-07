/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.idpStatistic;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.base.endpoint.idp.IdpStatistic;
import pl.edu.icm.unity.base.endpoint.idp.IdpStatistic.Status;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.IdpStatisticManagement;
import pl.edu.icm.unity.engine.api.IdpStatisticManagement.GroupBy;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic;

public class IdpStatisticsManagementTest extends DBIntegrationTestBase
{
	@Autowired
	private IdpStatisticManagement statMan;

	@BeforeEach
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
		assertThat(idpStatisticsSince).hasSize(1);
		assertThat(idpStatisticsSince.get(0)).isEqualTo(s1);
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
		assertThat(idpStatisticsSince).hasSize(2);
		assertThat(idpStatisticsSince.contains(s1)).isEqualTo(true);
		assertThat(idpStatisticsSince.contains(s2)).isEqualTo(true);
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
				.getIdpStatisticsSinceGroupBy(LocalDateTime.now().minusDays(10), GroupBy.total, 1000, false);
		assertThat(idpStatisticsSince).hasSize(1);
		assertThat(idpStatisticsSince.get(0).idpId).isEqualTo("eid");
		assertThat(idpStatisticsSince.get(0).clientId).isEqualTo("c");
		assertThat(idpStatisticsSince.get(0).sigInStats.get(0).failedCount).isEqualTo(1L);
		assertThat(idpStatisticsSince.get(0).sigInStats.get(0).successfullCount).isEqualTo(3L);
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
		assertThat(idpStatisticsSince).hasSize(1);
		assertThat(idpStatisticsSince.get(0)).isEqualTo(s1);
	}

}
