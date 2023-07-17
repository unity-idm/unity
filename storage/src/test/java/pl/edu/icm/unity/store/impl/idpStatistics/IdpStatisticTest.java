/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.idpStatistics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.endpoint.idp.IdpStatistic;
import pl.edu.icm.unity.base.endpoint.idp.IdpStatistic.Status;
import pl.edu.icm.unity.store.api.BasicCRUDDAO;
import pl.edu.icm.unity.store.api.IdpStatisticDAO;
import pl.edu.icm.unity.store.impl.AbstractBasicDAOTest;

public class IdpStatisticTest extends AbstractBasicDAOTest<IdpStatistic>
{

	@Autowired
	private IdpStatisticDAO dao;

	@Override
	protected BasicCRUDDAO<IdpStatistic> getDAO()
	{
		return dao;
	}

	@Override
	protected IdpStatistic getObject(String id)
	{
		return IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("ename").clientId("cid")
				.clientName("cname").status(Status.SUCCESSFUL).timestamp(LocalDateTime.now())
				.build();
	}

	@Override
	protected IdpStatistic mutateObject(IdpStatistic src)
	{
		return IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("ename").clientId("cid")
				.clientName("cname").status(Status.FAILED).timestamp(LocalDateTime.now()).build();
	}

	@Test
	public void shouldRemoveStatisticsOldStatistics()
	{
		tx.runInTransaction(() ->
		{
			IdpStatistic s1 = IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("ename").clientId("cid")
					.clientName("cname").status(Status.SUCCESSFUL).timestamp(LocalDateTime.now())
					.build();

			IdpStatistic s2 = IdpStatistic.builder().idpEndpointId("eid2").idpEndpointName("ename2").clientId("cid")
					.clientName("cname").status(Status.SUCCESSFUL).timestamp(LocalDateTime.now())
					.build();

			IdpStatistic toRemove = IdpStatistic.builder().idpEndpointId("eid2").idpEndpointName("ename2")
					.clientId("cid").clientName("cname").status(Status.SUCCESSFUL)
					.timestamp(LocalDateTime.now().minusDays(2)).build();
			dao.create(s1);
			dao.create(s2);
			dao.create(toRemove);

			dao.deleteOlderThan(LocalDateTime.now().minusDays(1));

			List<IdpStatistic> all = dao.getAll();
			assertThat(all).hasSize(2);
			assertThat(all).contains(s1);
			assertThat(all).contains(s2);
		});
	}

	@Test
	public void shouldGetStatisticsFromPeriod()
	{
		tx.runInTransaction(() ->
		{
			IdpStatistic s1 = IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("ename").clientId("cid")
					.clientName("cname").status(Status.SUCCESSFUL).timestamp(LocalDateTime.now())
					.build();

			IdpStatistic s2 = IdpStatistic.builder().idpEndpointId("eid2").idpEndpointName("ename2").clientId("cid")
					.clientName("cname").status(Status.SUCCESSFUL).timestamp(LocalDateTime.now())
					.build();

			IdpStatistic toSkip = IdpStatistic.builder().idpEndpointId("eid2").idpEndpointName("ename2").clientId("cid")
					.clientName("cname").status(Status.SUCCESSFUL)
					.timestamp(LocalDateTime.now().minusDays(2)).build();
			dao.create(s1);
			dao.create(s2);
			dao.create(toSkip);

			List<IdpStatistic> all = dao.getIdpStatistics(LocalDateTime.now().minusDays(1),
					LocalDateTime.now(), 100);
			assertThat(all).hasSize(2);
			assertThat(all).contains(s1);
			assertThat(all).contains(s2);
		});
	}

	@Override
	public void shouldReturnUpdatedByKey()
	{
		// Update not supported
	}

	@Override
	public void shouldFailOnUpdatingAbsent()
	{
		// Update not supported
	}

}
