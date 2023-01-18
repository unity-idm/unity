/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.type.TypeReference;

import io.imunity.rest.api.types.idp.statistic.RestGroupedIdpStatistic;
import pl.edu.icm.unity.engine.api.IdpStatisticManagement;
import pl.edu.icm.unity.engine.api.IdpStatisticManagement.GroupBy;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic;

public class TestIdpStatistics extends RESTAdminTestBase
{
	@Autowired
	protected IdpStatisticManagement idpStatisticManagement;

	@Test
	public void shouldReturnStatistics() throws Exception
	{
		idpStatisticManagement.addIdpStatistic(IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("ename")
				.clientId("c1").clientName("cName")
				.timestamp(LocalDateTime.now())
				.status(pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic.Status.SUCCESSFUL).build());

		idpStatisticManagement.addIdpStatistic(IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("ename")
				.clientId("c1").clientName("cName")
				.timestamp(LocalDateTime.now())
				.status(pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic.Status.FAILED).build());

		HttpGet get = new HttpGet(
				"/restadm/v1/idp-stats?since=" + Date.from(Instant.now().minusSeconds(1000)).toInstant().toEpochMilli()
						+ "&groupBy=" + GroupBy.total.toString());
		String contents = client.execute(host, get, getClientContext(host), new BasicHttpClientResponseHandler());
		System.out.println("Response:\n" + contents);
		List<RestGroupedIdpStatistic> returnedL = m.readValue(contents, new TypeReference<List<RestGroupedIdpStatistic>>()
		{
		});
		assertThat(returnedL.size(), is(1));
		assertThat(returnedL.get(0).idpId, is("eid"));
		assertThat(returnedL.get(0).idpName, is("ename"));
		assertThat(returnedL.get(0).clientId, is("c1"));
		assertThat(returnedL.get(0).clientName, is("cName"));
		assertThat(returnedL.get(0).sigInStats.size(), is(1));
		assertThat(returnedL.get(0).sigInStats.get(0).failedCount, is(1L));
		assertThat(returnedL.get(0).sigInStats.get(0).successfullCount, is(1L));

	}
}
