/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.idpStatistic;

import static org.assertj.core.api.Assertions.assertThat;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.base.endpoint.idp.IdpStatistic;
import pl.edu.icm.unity.base.endpoint.idp.IdpStatistic.Status;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.IdpStatisticManagement.GroupBy;
import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic;

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
				.groupBy(LocalDateTime.now().minusMonths(12), input, GroupBy.month, 100, false);

		assertThat(idpStatisticsByMonth).hasSize(10);
		assertThat(idpStatisticsByMonth.get(0).sigInStats).hasSize(13);
		assertThat(idpStatisticsByMonth.get(0).sigInStats.get(12).totatCount).isEqualTo(2L);
		assertThat(idpStatisticsByMonth.get(0).sigInStats.get(12).successfullCount).isEqualTo(1L);
		assertThat(idpStatisticsByMonth.get(0).sigInStats.get(12).failedCount).isEqualTo(1L);

		assertThat(idpStatisticsByMonth.get(9).sigInStats.get(3).totatCount).isEqualTo(2L);
		assertThat(idpStatisticsByMonth.get(9).sigInStats.get(3).successfullCount).isEqualTo(1L);
		assertThat(idpStatisticsByMonth.get(9).sigInStats.get(3).failedCount).isEqualTo(1L);

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
				.groupBy(LocalDateTime.now().minusDays(12), input, GroupBy.day, 100, false);

		assertThat(idpStatisticsByDay).hasSize(10);
		assertThat(idpStatisticsByDay.get(0).sigInStats).hasSize(13);
		assertThat(idpStatisticsByDay.get(0).sigInStats.get(12).totatCount).isEqualTo(2L);
		assertThat(idpStatisticsByDay.get(0).sigInStats.get(12).successfullCount).isEqualTo(1L);
		assertThat(idpStatisticsByDay.get(0).sigInStats.get(12).failedCount).isEqualTo(1L);

		assertThat(idpStatisticsByDay.get(9).sigInStats.get(3).totatCount).isEqualTo(2L);
		assertThat(idpStatisticsByDay.get(9).sigInStats.get(3).successfullCount).isEqualTo(1L);
		assertThat(idpStatisticsByDay.get(9).sigInStats.get(3).failedCount).isEqualTo(1L);

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
				input, GroupBy.none, 100, false);

		assertThat(idpStatistics).hasSize(10);
		assertThat(idpStatistics.get(0).sigInStats).hasSize(2);
		assertThat(idpStatistics.get(0).sigInStats.get(0).totatCount).isEqualTo(1L);
		assertThat(idpStatistics.get(0).sigInStats.get(0).successfullCount).isEqualTo(1L);
		assertThat(idpStatistics.get(0).sigInStats.get(0).failedCount).isEqualTo(0L);

		assertThat(idpStatistics.get(0).sigInStats.get(1).totatCount).isEqualTo(1L);
		assertThat(idpStatistics.get(0).sigInStats.get(1).successfullCount).isEqualTo(0L);
		assertThat(idpStatistics.get(0).sigInStats.get(1).failedCount).isEqualTo(1L);
	}
	
	@Test
	public void shouldLimitSigIntStat() throws EngineException, JsonProcessingException
	{
		List<IdpStatistic> input = new ArrayList<>();
		for (int i = 0; i < 10; i++)
		{
			input.add(IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("ename" + i).clientId("c")
					.clientName("cName" + i).timestamp(LocalDateTime.now().minusDays(i)).status(Status.SUCCESSFUL)
					.build());

			input.add(IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("ename" + i).clientId("c")
					.clientName("cName" + i).timestamp(LocalDateTime.now().minusDays(i)).status(Status.FAILED).build());
		}

		List<GroupedIdpStatistic> idpStatistics = IdpStatisticGroupingHelper.groupBy(LocalDateTime.now().minusDays(12),
				input, GroupBy.none, 5, false);

		assertThat(idpStatistics).hasSize(1);
		assertThat(idpStatistics.get(0).sigInStats).hasSize(5);
	}
	
	@Test
	public void shouldSkipZeroRecordLimitSigIntStat() throws EngineException, JsonProcessingException
	{
		List<IdpStatistic> input = new ArrayList<>();
		for (int i = 0; i < 10; i++)
		{
			input.add(IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("ename").clientId("c")
					.clientName("cName").timestamp(LocalDateTime.now().minusDays(1)).status(Status.SUCCESSFUL)
					.build());

			input.add(IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("ename").clientId("c")
					.clientName("cName").timestamp(LocalDateTime.now().minusDays(1)).status(Status.FAILED).build());
		}

		List<GroupedIdpStatistic> idpStatistics = IdpStatisticGroupingHelper.groupBy(LocalDateTime.now().minusDays(12),
				input, GroupBy.day, 100, true);

		assertThat(idpStatistics).hasSize(1);
		assertThat(idpStatistics.get(0).sigInStats).hasSize(1);
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
				input, GroupBy.total, 100, false);

		assertThat(idpStatistics).hasSize(2);
		assertThat(idpStatistics.get(0).sigInStats).hasSize(1);
		assertThat(idpStatistics.get(0).sigInStats.get(0).totatCount).isEqualTo(10L);
		assertThat(idpStatistics.get(0).sigInStats.get(0).successfullCount).isEqualTo(5L);
		assertThat(idpStatistics.get(0).sigInStats.get(0).failedCount).isEqualTo(5L);

		assertThat(idpStatistics.get(1).sigInStats.get(0).totatCount).isEqualTo(10L);
		assertThat(idpStatistics.get(1).sigInStats.get(0).successfullCount).isEqualTo(5L);
		assertThat(idpStatistics.get(1).sigInStats.get(0).failedCount).isEqualTo(5L);

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
				input, GroupBy.total, 100, false);

		assertThat(idpStatistics.get(0).idpName).isEqualTo("eName0");
		assertThat(idpStatistics.get(0).clientName).isEqualTo("cName0");
	}

}
