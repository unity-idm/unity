/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.idpStatistics;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic.Status;

@Component
public class IdpStatisticJsonSerializer implements RDBMSObjectSerializer<IdpStatistic, IdpStatisticBean>
{

	@Override
	public IdpStatisticBean toDB(IdpStatistic object)
	{
		return new IdpStatisticBean(null, object.getTimestamp(), object.getIdpEndpointId(), object.getIdpEndpointName(),
				object.getClientId(), object.getClientName(), object.getStatus().toString());
	}

	@Override
	public IdpStatistic fromDB(IdpStatisticBean bean)
	{
		return IdpStatistic.builder().clientId(bean.getClientId()).clientName(bean.getClientName())
				.idpEndpointId(bean.getIdpEndpointId()).idpEndpointName(bean.getIdpEndpointName())
				.status(Status.valueOf(bean.getStatus())).timestamp(bean.getTimestamp()).build();
	}

}
