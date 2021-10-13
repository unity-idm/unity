/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.type.TypeReference;

import pl.edu.icm.unity.engine.api.IdpStatisticManagement;
import pl.edu.icm.unity.engine.api.IdpStatisticManagement.GroupBy;
import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic;
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
				.timestamp(Date.from(LocalDateTime.now().toInstant(OffsetDateTime.now().getOffset())))
				.status(pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic.Status.SUCCESSFUL).build());

		idpStatisticManagement.addIdpStatistic(IdpStatistic.builder().idpEndpointId("eid").idpEndpointName("ename")
				.clientId("c1").clientName("cName")
				.timestamp(Date.from(LocalDateTime.now().toInstant(OffsetDateTime.now().getOffset())))
				.status(pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic.Status.FAILED).build());

		HttpGet get = new HttpGet(
				"/restadm/v1/idp-stats?when=" + Date.from(Instant.now().minusSeconds(1000)).toInstant().toEpochMilli()
						+ "&groupBy=" + GroupBy.total.toString());
		HttpResponse responseGet = client.execute(host, get, localcontext);

		String contents = EntityUtils.toString(responseGet.getEntity());
		System.out.println("Response:\n" + contents);
		assertEquals(contents, Status.OK.getStatusCode(), responseGet.getStatusLine().getStatusCode());
		List<GroupedIdpStatistic> returnedL = m.readValue(contents, new TypeReference<List<GroupedIdpStatistic>>()
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
