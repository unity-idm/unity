/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.idpStatistic;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.engine.api.IdpStatisticManagement.GroupBy;
import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic.Status;

public class IdpStatisticGroupingHelperTest
{
	@Test
	public void shouldReturnGroupedByMonth() throws EngineException, JsonProcessingException
	{
		List<IdpStatistic> input = new ArrayList<>();

		for (int i = 0; i < 10; i++)
		{
			input.add(IdpStatistic.builder().idpEndpointId("eid" + i).idpEndpointName("ename").clientId("c1")
					.clientName("cName").timestamp(LocalDateTime.now().minusMonths(i)).status(Status.SUCCESSFUL)
					.build());

			input.add(IdpStatistic.builder().idpEndpointId("eid" + i).idpEndpointName("ename").clientId("c1")
					.clientName("cName").timestamp(LocalDateTime.now().minusMonths(i)).status(Status.FAILED).build());
		}

		List<GroupedIdpStatistic> idpStatisticsByMonth = IdpStatisticGroupingHelper
				.groupBy(LocalDateTime.now().minusMonths(12), input, GroupBy.month);

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
		List<IdpStatistic> input = new ArrayList<>();
		for (int i = 0; i < 10; i++)
		{
			input.add(IdpStatistic.builder().idpEndpointId("eid" + i).idpEndpointName("ename").clientId("c1")
					.clientName("cName").timestamp(LocalDateTime.now().minusDays(i)).status(Status.SUCCESSFUL).build());

			input.add(IdpStatistic.builder().idpEndpointId("eid" + i).idpEndpointName("ename").clientId("c1")
					.clientName("cName").timestamp(LocalDateTime.now().minusDays(i)).status(Status.FAILED).build());
		}

		List<GroupedIdpStatistic> idpStatisticsByDay = IdpStatisticGroupingHelper
				.groupBy(LocalDateTime.now().minusDays(12), input, GroupBy.day);

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
		List<IdpStatistic> input = new ArrayList<>();
		for (int i = 0; i < 10; i++)
		{
			input.add(IdpStatistic.builder().idpEndpointId("eid" + i).idpEndpointName("ename" + i).clientId("c" + i)
					.clientName("cName" + i).timestamp(LocalDateTime.now().minusDays(i)).status(Status.SUCCESSFUL)
					.build());

			input.add(IdpStatistic.builder().idpEndpointId("eid" + i).idpEndpointName("ename" + i).clientId("c" + i)
					.clientName("cName" + i).timestamp(LocalDateTime.now().minusDays(i)).status(Status.FAILED).build());
		}

		List<GroupedIdpStatistic> idpStatistics = IdpStatisticGroupingHelper.groupBy(LocalDateTime.now().minusDays(12),
				input, GroupBy.none);

		assertThat(idpStatistics.size(), is(10));
		assertThat(idpStatistics.get(0).sigInStats.size(), is(2));
		assertThat(idpStatistics.get(0).sigInStats.get(0).totatCount, is(1L));
		assertThat(idpStatistics.get(0).sigInStats.get(0).successfullCount, is(1L));
		assertThat(idpStatistics.get(0).sigInStats.get(0).failedCount, is(0L));

		assertThat(idpStatistics.get(0).sigInStats.get(1).totatCount, is(1L));
		assertThat(idpStatistics.get(0).sigInStats.get(1).successfullCount, is(0L));
		assertThat(idpStatistics.get(0).sigInStats.get(1).failedCount, is(1L));
	}

	@Test
	public void shouldReturnTotal() throws EngineException, JsonProcessingException
	{
		List<IdpStatistic> input = new ArrayList<>();
		for (int i = 0; i < 10; i++)
		{
			input.add(IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("ename").clientId("c")
					.clientName("cName").timestamp(LocalDateTime.now().minusSeconds(i))
					.status(i < 5 ? Status.FAILED : Status.SUCCESSFUL).build());

			input.add(IdpStatistic.builder().idpEndpointId("eid1").idpEndpointName("ename1").clientId("c")
					.clientName("cName").timestamp(LocalDateTime.now().minusSeconds(i))
					.status(i < 5 ? Status.FAILED : Status.SUCCESSFUL).build());

		}

		List<GroupedIdpStatistic> idpStatistics = IdpStatisticGroupingHelper.groupBy(LocalDateTime.now().minusDays(12),
				input, GroupBy.total);

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
		List<IdpStatistic> input = new ArrayList<>();
		for (int i = 0; i < 10; i++)
		{
			input.add(IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("eName" + i).clientId("c")
					.clientName("cName" + i).timestamp(LocalDateTime.now().minusSeconds(i))
					.status(i < 5 ? Status.FAILED : Status.SUCCESSFUL).build());

		}
		List<GroupedIdpStatistic> idpStatistics = IdpStatisticGroupingHelper.groupBy(LocalDateTime.now().minusDays(12),
				input, GroupBy.total);

		assertThat(idpStatistics.get(0).idpName, is("eName0"));
		assertThat(idpStatistics.get(0).clientName, is("cName0"));
	}

}
