/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.idpStatistics;

import java.sql.Timestamp;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.endpoint.idp.IdpStatistic;
import pl.edu.icm.unity.base.endpoint.idp.IdpStatistic.Status;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;

@Component
public class IdpStatisticJsonSerializer implements RDBMSObjectSerializer<IdpStatistic, IdpStatisticBean>
{

	@Override
	public IdpStatisticBean toDB(IdpStatistic object)
	{
		return new IdpStatisticBean(null, Timestamp.valueOf(object.timestamp), object.idpEndpointId,
				object.idpEndpointName, object.clientId, object.clientName, object.status.toString());
	}

	@Override
	public IdpStatistic fromDB(IdpStatisticBean bean)
	{
		return IdpStatistic.builder().clientId(bean.getClientId()).clientName(bean.getClientName())
				.idpEndpointId(bean.getIdpEndpointId()).idpEndpointName(bean.getIdpEndpointName())
				.status(Status.valueOf(bean.getStatus()))
				.timestamp(bean.getTimestamp().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()).build();
	}

}
