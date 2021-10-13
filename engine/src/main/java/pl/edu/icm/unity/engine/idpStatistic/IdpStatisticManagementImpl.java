/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.idpStatistic;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.IdpStatisticManagement;
import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic;
import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic.SigInStatistic;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.IdpStatisticDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic.Status;

@Component
public class IdpStatisticManagementImpl implements IdpStatisticManagement
{
	private final IdpStatisticDAO dao;
	private final InternalAuthorizationManager authz;

	public IdpStatisticManagementImpl(IdpStatisticDAO dao, InternalAuthorizationManager authz)
	{
		this.dao = dao;
		this.authz = authz;
	}

	@Override
	@Transactional
	public List<IdpStatistic> getIdpStatisticsSince(Date since) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return dao.getIdpStatistics(since, Date.from(Instant.now()), 10000);
	}

	@Override
	@Transactional
	public void deleteOlderThan(Date olderThan) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		dao.deleteOlderThan(olderThan);
	}

	@Override
	@Transactional
	public void addIdpStatistic(IdpStatistic toAdd) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		dao.create(toAdd);
	}

	@Override
	@Transactional
	public List<GroupedIdpStatistic> getIdpStatisticsSinceGroupBy(Date since, GroupBy groupBy) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		List<IdpStatistic> idpStatistics = dao.getIdpStatistics(since, Date.from(Instant.now()), 10000);
		Map<IdpEndpointAndClient, List<IdpStatistic>> byEndpointAndClient = idpStatistics.stream().collect(Collectors
				.groupingBy(s -> new IdpEndpointAndClient(s.getIdpEndpointId(), s.getClientId()), Collectors.toList()));
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

	private List<GroupedIdpStatistic> getNotGrouped(Map<IdpEndpointAndClient, List<IdpStatistic>> byEndpointAndClient)
	{
		List<GroupedIdpStatistic> retStats = new ArrayList<>();
		for (Entry<IdpEndpointAndClient, List<IdpStatistic>> entry : byEndpointAndClient.entrySet())
		{
			IdpEndpointAndClient key = entry.getKey();
			ArrayList<SigInStatistic> stats = new ArrayList<>();
			for (IdpStatistic s : entry.getValue())
			{
				stats.add(new SigInStatistic(s.getTimestamp(), s.getTimestamp(), 1,
						s.getStatus().equals(Status.SUCCESSFUL) ? 1 : 0, s.getStatus().equals(Status.FAILED) ? 1 : 0));
			}
			retStats.add(new GroupedIdpStatistic(key.endpointId, getLastEndpointName(entry.getValue()), key.clientId,
					getLastClientName(entry.getValue()), stats));
		}
		return retStats;
	}

	private String getLastEndpointName(List<IdpStatistic> stats)
	{
		return stats.stream().sorted(Comparator.comparing(IdpStatistic::getTimestamp).reversed()).findFirst().get()
				.getIdpEndpointName();
	}

	private String getLastClientName(List<IdpStatistic> stats)
	{
		return stats.stream().sorted(Comparator.comparing(IdpStatistic::getTimestamp).reversed()).findFirst().get()
				.getClientName();
	}

	private List<GroupedIdpStatistic> getTotal(Map<IdpEndpointAndClient, List<IdpStatistic>> byEndpointAndClient,
			Date since)
	{
		List<GroupedIdpStatistic> retStats = new ArrayList<>();
		for (Entry<IdpEndpointAndClient, List<IdpStatistic>> entry : byEndpointAndClient.entrySet())
		{
			IdpEndpointAndClient endpointAndClient = entry.getKey();
			ArrayList<SigInStatistic> stats = new ArrayList<>();
			stats.add(new SigInStatistic(since, Date.from(Instant.now()), entry.getValue().size(),
					entry.getValue().stream().filter(s -> s.getStatus().equals(Status.SUCCESSFUL)).count(),
					entry.getValue().stream().filter(s -> s.getStatus().equals(Status.FAILED)).count()

			));
			retStats.add(new GroupedIdpStatistic(endpointAndClient.endpointId, getLastEndpointName(entry.getValue()), endpointAndClient.clientId,
					getLastClientName(entry.getValue()), stats));
		}
		return retStats;
	}

	private List<GroupedIdpStatistic> getByMonth(Map<IdpEndpointAndClient, List<IdpStatistic>> byEndpointAndClient,
			Date since)
	{
		List<GroupedIdpStatistic> groupedStats = new ArrayList<>();
		for (Entry<IdpEndpointAndClient, List<IdpStatistic>> entry : byEndpointAndClient.entrySet())
		{
			IdpEndpointAndClient endpointAndClient = entry.getKey();
			ArrayList<SigInStatistic> stats = new ArrayList<>();
			Map<YearMonth, List<IdpStatistic>> byMonth = entry.getValue().stream().collect(Collectors.groupingBy(
					d -> YearMonth.from(d.getTimestamp().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())));
			YearMonth actualYearMonth = YearMonth.from(LocalDateTime.now());
			YearMonth loopMonth = YearMonth.from(since.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
			do
			{
				List<IdpStatistic> byMonthEntry = byMonth.getOrDefault(loopMonth, new ArrayList<>());
				stats.add(
						new SigInStatistic(
								YearMonth.from(since.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
										.equals(loopMonth)
												? since
												: Date.from(loopMonth.atDay(1).atStartOfDay()
														.atZone(ZoneId.systemDefault()).toInstant()),
								Date.from(loopMonth.atEndOfMonth().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault())
										.toInstant()),
								byMonthEntry.size(),
								byMonthEntry.stream().filter(s -> s.getStatus().equals(Status.SUCCESSFUL)).count(),
								byMonthEntry.stream().filter(s -> s.getStatus().equals(Status.FAILED)).count()));

				loopMonth = loopMonth.plusMonths(1);
			} while (!loopMonth.isAfter(actualYearMonth));
			groupedStats.add(new GroupedIdpStatistic(endpointAndClient.endpointId, getLastEndpointName(entry.getValue()),
					endpointAndClient.clientId, getLastClientName(entry.getValue()), stats));
		}

		return groupedStats;
	}

	private List<GroupedIdpStatistic> getByDay(Map<IdpEndpointAndClient, List<IdpStatistic>> byEndpointAndClient,
			Date since)
	{
		List<GroupedIdpStatistic> groupedStats = new ArrayList<>();
		for (Entry<IdpEndpointAndClient, List<IdpStatistic>> entry : byEndpointAndClient.entrySet())
		{
			IdpEndpointAndClient endpointAndClient = entry.getKey();
			ArrayList<SigInStatistic> stats = new ArrayList<>();
			Map<MonthDay, List<IdpStatistic>> byDay = entry.getValue().stream().collect(Collectors.groupingBy(
					d -> MonthDay.from(d.getTimestamp().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())));
			LocalDate now = LocalDate.now();
			LocalDate loopDate = since.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			do
			{
				MonthDay loopDay = MonthDay.from(loopDate);
				List<IdpStatistic> byDayEntry = byDay.getOrDefault(loopDay, new ArrayList<>());
				stats.add(new SigInStatistic(
						MonthDay.from(since.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()).equals(loopDay)
								? since
								: Date.from(loopDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
						Date.from(loopDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()),
						byDayEntry.size(), byDayEntry.stream().filter(s -> s.getStatus().equals(Status.SUCCESSFUL)).count(),
						byDayEntry.stream().filter(s -> s.getStatus().equals(Status.FAILED)).count()));

				loopDate = loopDate.plusDays(1);
			} while (!loopDate.isAfter(now));
			groupedStats.add(new GroupedIdpStatistic(endpointAndClient.endpointId, getLastEndpointName(entry.getValue()),
					endpointAndClient.clientId, getLastClientName(entry.getValue()), stats));
		}
		return groupedStats;
	}

	private static class IdpEndpointAndClient
	{
		public String endpointId;
		public String clientId;

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
