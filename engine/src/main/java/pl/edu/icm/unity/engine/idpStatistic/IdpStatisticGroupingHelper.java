/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.idpStatistic;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import pl.edu.icm.unity.engine.api.IdpStatisticManagement.GroupBy;
import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic;
import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic.SigInStatistic;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic.Status;

class IdpStatisticGroupingHelper
{
	static List<GroupedIdpStatistic> groupBy(LocalDateTime since, List<IdpStatistic> stats, GroupBy groupBy)
	{
		Map<IdpEndpointAndClient, List<IdpStatistic>> byEndpointAndClient = stats.stream().collect(
				Collectors.groupingBy(s -> new IdpEndpointAndClient(s.idpEndpointId, s.clientId), Collectors.toList()));
		switch (groupBy)
		{
		case none:
			return getNotGrouped(byEndpointAndClient);
		case month:
			return getByMonth(byEndpointAndClient, since);
		case day:
			return getByDay(byEndpointAndClient, since);
		default:
			return getTotal(byEndpointAndClient, since);
		}
	}

	private static List<GroupedIdpStatistic> getTotal(Map<IdpEndpointAndClient, List<IdpStatistic>> byEndpointAndClient,
			LocalDateTime since)
	{
		List<GroupedIdpStatistic> retStats = new ArrayList<>();
		for (Entry<IdpEndpointAndClient, List<IdpStatistic>> entry : byEndpointAndClient.entrySet())
		{
			IdpEndpointAndClient endpointAndClient = entry.getKey();
			List<SigInStatistic> stats = new ArrayList<>();
			stats.add(new SigInStatistic(since, LocalDateTime.now(), entry.getValue().size(),
					entry.getValue().stream().filter(s -> s.status.equals(Status.SUCCESSFUL)).count(),
					entry.getValue().stream().filter(s -> s.status.equals(Status.FAILED)).count()

			));
			retStats.add(new GroupedIdpStatistic(endpointAndClient.endpointId, getLastEndpointName(entry.getValue()),
					endpointAndClient.clientId, getLastClientName(entry.getValue()), stats));
		}
		return retStats;
	}

	private static List<GroupedIdpStatistic> getNotGrouped(
			Map<IdpEndpointAndClient, List<IdpStatistic>> byEndpointAndClient)
	{
		List<GroupedIdpStatistic> retStats = new ArrayList<>();
		for (Entry<IdpEndpointAndClient, List<IdpStatistic>> entry : byEndpointAndClient.entrySet())
		{
			IdpEndpointAndClient key = entry.getKey();
			List<SigInStatistic> stats = new ArrayList<>();
			for (IdpStatistic s : entry.getValue())
			{
				stats.add(new SigInStatistic(s.timestamp, s.timestamp, 1, s.status.equals(Status.SUCCESSFUL) ? 1 : 0,
						s.status.equals(Status.FAILED) ? 1 : 0));
			}
			retStats.add(new GroupedIdpStatistic(key.endpointId, getLastEndpointName(entry.getValue()), key.clientId,
					getLastClientName(entry.getValue()), stats));
		}
		return retStats;
	}

	private static List<GroupedIdpStatistic> getByMonth(
			Map<IdpEndpointAndClient, List<IdpStatistic>> byEndpointAndClient, LocalDateTime since)
	{

		return getWithPeriodsAdjuster(byEndpointAndClient, since, d -> d.toLocalDate().withDayOfMonth(1).atStartOfDay(),
				d -> d.toLocalDate().withDayOfMonth(d.toLocalDate().lengthOfMonth()).atTime(LocalTime.MAX));

	}

	private static List<GroupedIdpStatistic> getByDay(Map<IdpEndpointAndClient, List<IdpStatistic>> byEndpointAndClient,
			LocalDateTime since)
	{
		return getWithPeriodsAdjuster(byEndpointAndClient, since, d -> d.toLocalDate().atStartOfDay(),
				d -> d.toLocalDate().atTime(LocalTime.MAX));
	}

	private static List<GroupedIdpStatistic> getWithPeriodsAdjuster(
			Map<IdpEndpointAndClient, List<IdpStatistic>> byEndpointAndClient, LocalDateTime since,
			Function<LocalDateTime, LocalDateTime> adjuster, Function<LocalDateTime, LocalDateTime> endAdjuster)
	{
		List<GroupedIdpStatistic> groupedStats = new ArrayList<>();
		for (Entry<IdpEndpointAndClient, List<IdpStatistic>> entry : byEndpointAndClient.entrySet())
		{
			IdpEndpointAndClient endpointAndClient = entry.getKey();
			ArrayList<SigInStatistic> stats = new ArrayList<>();
			Map<LocalDateTime, List<IdpStatistic>> byPeriod = entry.getValue().stream()
					.collect(Collectors.groupingBy(d -> adjuster.apply(d.timestamp)));

			LocalDateTime actual = LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX);
			LocalDateTime loopDate = adjuster.apply(since);

			do
			{
				List<IdpStatistic> byMonthEntry = byPeriod.getOrDefault(adjuster.apply(loopDate), new ArrayList<>());
				stats.add(new SigInStatistic(
						adjuster.apply(since).equals(loopDate) ? since
								: adjuster.apply(loopDate).toLocalDate().atStartOfDay(),
						endAdjuster.apply(loopDate).toLocalDate().atTime(LocalTime.MAX), byMonthEntry.size(),
						byMonthEntry.stream().filter(s -> s.status.equals(Status.SUCCESSFUL)).count(),
						byMonthEntry.stream().filter(s -> s.status.equals(Status.FAILED)).count()));

				loopDate = endAdjuster.apply(loopDate).plusDays(1);
			} while (!loopDate.isAfter(actual));
			groupedStats
					.add(new GroupedIdpStatistic(endpointAndClient.endpointId, getLastEndpointName(entry.getValue()),
							endpointAndClient.clientId, getLastClientName(entry.getValue()), stats));
		}

		return groupedStats;
	}

	private static String getLastEndpointName(List<IdpStatistic> stats)
	{
		return stats.stream().sorted((s1, s2) -> s2.timestamp.compareTo(s1.timestamp)).findFirst()
				.get().idpEndpointName;
	}

	private static String getLastClientName(List<IdpStatistic> stats)
	{
		return stats.stream().sorted((s1, s2) -> s2.timestamp.compareTo(s1.timestamp)).findFirst().get().clientName;
	}

	private static class IdpEndpointAndClient
	{
		private final String endpointId;
		private final String clientId;

		public IdpEndpointAndClient(String endpointId, String clientId)
		{
			this.endpointId = endpointId;
			this.clientId = clientId;

		}

		@Override
		public int hashCode()
		{
			return Objects.hash(clientId, endpointId);
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			IdpEndpointAndClient other = (IdpEndpointAndClient) obj;
			return Objects.equals(clientId, other.clientId) && Objects.equals(endpointId, other.endpointId);
		}
	}
}
