/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.idpStatistic;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.IdpStatisticManagement;
import pl.edu.icm.unity.engine.api.IdpStatisticManagement.GroupBy;
import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic.Status;

public class IdpStatisticsManagementTest extends DBIntegrationTestBase
{

	@Autowired
	private IdpStatisticManagement statMan;

	
	@Before
	public void clear() throws EngineException
	{
		statMan.deleteOlderThan(new Date());
	}
	
	@Test
	public void shouldReturnGroupedByMonth() throws EngineException, JsonProcessingException
	{
		for (int i = 0; i < 10; i++)
		{
			statMan.addIdpStatistic(IdpStatistic.builder().idpEndpointId("eid" + i).idpEndpointName("ename")
					.clientId("c1").clientName("cName")
					.timestamp(
							Date.from(LocalDateTime.now().minusMonths(i).toInstant(OffsetDateTime.now().getOffset())))
					.status(Status.SUCCESSFUL).build());

			statMan.addIdpStatistic(IdpStatistic.builder().idpEndpointId("eid" + i).idpEndpointName("ename")
					.clientId("c1").clientName("cName")
					.timestamp(
							Date.from(LocalDateTime.now().minusMonths(i).toInstant(OffsetDateTime.now().getOffset())))
					.status(Status.FAILED).build());
		}

		List<GroupedIdpStatistic> idpStatisticsByMonth = statMan.getIdpStatisticsSinceGroupBy(
				Date.from(LocalDateTime.now().minusMonths(12).toInstant(OffsetDateTime.now().getOffset())),
				GroupBy.month);

		assertThat(idpStatisticsByMonth.size(), is(10));
		assertThat(idpStatisticsByMonth.get(0).sigInStats.size(), is(13));
		assertThat(idpStatisticsByMonth.get(0).sigInStats.get(12).totatCount, is(2L));
		assertThat(idpStatisticsByMonth.get(0).sigInStats.get(12).successfullCount, is(1L));
		assertThat(idpStatisticsByMonth.get(0).sigInStats.get(12).failedCount, is(1L));

		assertThat(idpStatisticsByMonth.get(9).sigInStats.get(3).totatCount, is(2L));
		assertThat(idpStatisticsByMonth.get(9).sigInStats.get(3).successfullCount, is(1L));
		assertThat(idpStatisticsByMonth.get(9).sigInStats.get(3).failedCount, is(1L));

	}

	@Test
	public void shouldReturnGroupedByDay() throws EngineException, JsonProcessingException
	{
		for (int i = 0; i < 10; i++)
		{
			statMan.addIdpStatistic(IdpStatistic.builder().idpEndpointId("eid" + i).idpEndpointName("ename")
					.clientId("c1").clientName("cName")
					.timestamp(Date.from(LocalDateTime.now().minusDays(i).toInstant(OffsetDateTime.now().getOffset())))
					.status(Status.SUCCESSFUL).build());

			statMan.addIdpStatistic(IdpStatistic.builder().idpEndpointId("eid" + i).idpEndpointName("ename")
					.clientId("c1").clientName("cName")
					.timestamp(Date.from(LocalDateTime.now().minusDays(i).toInstant(OffsetDateTime.now().getOffset())))
					.status(Status.FAILED).build());
		}

		List<GroupedIdpStatistic> idpStatisticsByDay = statMan.getIdpStatisticsSinceGroupBy(
				Date.from(LocalDateTime.now().minusDays(12).toInstant(OffsetDateTime.now().getOffset())), GroupBy.day);

		assertThat(idpStatisticsByDay.size(), is(10));
		assertThat(idpStatisticsByDay.get(0).sigInStats.size(), is(13));
		assertThat(idpStatisticsByDay.get(0).sigInStats.get(12).totatCount, is(2L));
		assertThat(idpStatisticsByDay.get(0).sigInStats.get(12).successfullCount, is(1L));
		assertThat(idpStatisticsByDay.get(0).sigInStats.get(12).failedCount, is(1L));

		assertThat(idpStatisticsByDay.get(9).sigInStats.get(3).totatCount, is(2L));
		assertThat(idpStatisticsByDay.get(9).sigInStats.get(3).successfullCount, is(1L));
		assertThat(idpStatisticsByDay.get(9).sigInStats.get(3).failedCount, is(1L));

	}

	@Test
	public void shouldReturnNotGrouped() throws EngineException, JsonProcessingException
	{
		for (int i = 0; i < 10; i++)
		{
			statMan.addIdpStatistic(IdpStatistic.builder().idpEndpointId("eid" + i).idpEndpointName("ename" + i)
					.clientId("c" + i).clientName("cName" + i)
					.timestamp(Date.from(LocalDateTime.now().minusDays(i).toInstant(OffsetDateTime.now().getOffset())))
					.status(Status.SUCCESSFUL).build());

			statMan.addIdpStatistic(IdpStatistic.builder().idpEndpointId("eid" + i).idpEndpointName("ename" + i)
					.clientId("c" + i).clientName("cName" + i)
					.timestamp(Date.from(LocalDateTime.now().minusDays(i).toInstant(OffsetDateTime.now().getOffset())))
					.status(Status.FAILED).build());
		}

		List<GroupedIdpStatistic> idpStatistics = statMan.getIdpStatisticsSinceGroupBy(
				Date.from(LocalDateTime.now().minusDays(12).toInstant(OffsetDateTime.now().getOffset())), GroupBy.none);

		assertThat(idpStatistics.size(), is(10));
		assertThat(idpStatistics.get(0).sigInStats.size(), is(2));
		assertThat(idpStatistics.get(0).sigInStats.get(0).totatCount, is(1L));
		assertThat(idpStatistics.get(0).sigInStats.get(0).successfullCount, is(0L));
		assertThat(idpStatistics.get(0).sigInStats.get(0).failedCount, is(1L));

		assertThat(idpStatistics.get(0).sigInStats.get(1).totatCount, is(1L));
		assertThat(idpStatistics.get(0).sigInStats.get(1).successfullCount, is(1L));
		assertThat(idpStatistics.get(0).sigInStats.get(1).failedCount, is(0L));
	}

	@Test
	public void shouldReturnTotal() throws EngineException, JsonProcessingException
	{
		for (int i = 0; i < 10; i++)
		{
			statMan.addIdpStatistic(IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("ename")
					.clientId("c").clientName("cName")
					.timestamp(
							Date.from(LocalDateTime.now().minusSeconds(i).toInstant(OffsetDateTime.now().getOffset())))
					.status(i < 5 ? Status.FAILED : Status.SUCCESSFUL).build());

			statMan.addIdpStatistic(IdpStatistic.builder().idpEndpointId("eid1").idpEndpointName("ename1")
					.clientId("c").clientName("cName")
					.timestamp(
							Date.from(LocalDateTime.now().minusSeconds(i).toInstant(OffsetDateTime.now().getOffset())))
					.status(i < 5 ? Status.FAILED : Status.SUCCESSFUL).build());

		}

		List<GroupedIdpStatistic> idpStatistics = statMan.getIdpStatisticsSinceGroupBy(
				Date.from(LocalDateTime.now().minusDays(12).toInstant(OffsetDateTime.now().getOffset())),
				GroupBy.total);

		assertThat(idpStatistics.size(), is(2));
		assertThat(idpStatistics.get(0).sigInStats.size(), is(1));
		assertThat(idpStatistics.get(0).sigInStats.get(0).totatCount, is(10L));
		assertThat(idpStatistics.get(0).sigInStats.get(0).successfullCount, is(5L));
		assertThat(idpStatistics.get(0).sigInStats.get(0).failedCount, is(5L));

		assertThat(idpStatistics.get(1).sigInStats.get(0).totatCount, is(10L));
		assertThat(idpStatistics.get(1).sigInStats.get(0).successfullCount, is(5L));
		assertThat(idpStatistics.get(1).sigInStats.get(0).failedCount, is(5L));

	}

	@Test
	public void shouldGetLastClientNameAndIdpName() throws EngineException, JsonProcessingException
	{
		for (int i = 0; i < 10; i++)
		{
			statMan.addIdpStatistic(IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("eName" + i)
					.clientId("c").clientName("cName" + i)
					.timestamp(
							Date.from(LocalDateTime.now().minusSeconds(i).toInstant(OffsetDateTime.now().getOffset())))
					.status(i < 5 ? Status.FAILED : Status.SUCCESSFUL).build());

		}
		List<GroupedIdpStatistic> idpStatistics = statMan.getIdpStatisticsSinceGroupBy(
				Date.from(LocalDateTime.now().minusDays(12).toInstant(OffsetDateTime.now().getOffset())),
				GroupBy.total);

		assertThat(idpStatistics.get(0).idpName, is("eName0"));
		assertThat(idpStatistics.get(0).clientName, is("cName0"));
	}

	@Test
	public void shouldDropOlderThan() throws EngineException, JsonProcessingException
	{
		IdpStatistic s1 = IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("eName").clientId("c")
				.clientName("cName")
				.timestamp(Date.from(LocalDateTime.now().toInstant(OffsetDateTime.now().getOffset())))
				.status(Status.SUCCESSFUL).build();

		IdpStatistic s2 = IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("eName").clientId("c")
				.clientName("cName")
				.timestamp(Date.from(LocalDateTime.now().minusDays(2).toInstant(OffsetDateTime.now().getOffset())))
				.status(Status.SUCCESSFUL).build();

		IdpStatistic s3 = IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("eName").clientId("c")
				.clientName("cName")
				.timestamp(Date.from(LocalDateTime.now().minusDays(3).toInstant(OffsetDateTime.now().getOffset())))
				.status(Status.SUCCESSFUL).build();

		IdpStatistic s4 = IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("eName").clientId("c")
				.clientName("cName")
				.timestamp(Date.from(LocalDateTime.now().minusDays(4).toInstant(OffsetDateTime.now().getOffset())))
				.status(Status.SUCCESSFUL).build();

		statMan.addIdpStatistic(s1);
		statMan.addIdpStatistic(s2);
		statMan.addIdpStatistic(s3);
		statMan.addIdpStatistic(s4);

		statMan.deleteOlderThan(
				Date.from(LocalDateTime.now().minusDays(1).toInstant(OffsetDateTime.now().getOffset())));

		List<IdpStatistic> idpStatisticsSince = statMan.getIdpStatisticsSince(
				Date.from(LocalDateTime.now().minusDays(10).toInstant(OffsetDateTime.now().getOffset())));
		assertThat(idpStatisticsSince.size(), is(1));
		assertThat(idpStatisticsSince.get(0), is(s1));
	}

}
