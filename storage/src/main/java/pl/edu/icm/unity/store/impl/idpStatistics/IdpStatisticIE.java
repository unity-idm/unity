/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.idpStatistics;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.endpoint.idp.IdpStatistic;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.api.IdpStatisticDAO;
import pl.edu.icm.unity.store.export.AbstractIEBase;


@Component
public class IdpStatisticIE extends AbstractIEBase<IdpStatistic>
{
	public static final String IDP_STATISTIC_OBJECT_TYPE = "idpStatistics";
	
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, IdpStatisticIE.class);
	private IdpStatisticDAO dao;

	@Autowired
	public IdpStatisticIE(IdpStatisticDAO dao, ObjectMapper objectMapper)
	{
		super(12, IDP_STATISTIC_OBJECT_TYPE, objectMapper);
		this.dao = dao;
	}

	@Override
	protected List<IdpStatistic> getAllToExport()
	{
		return dao.getAll();
	}

	@Override
	protected ObjectNode toJsonSingle(IdpStatistic exportedObj)
	{
		return jsonMapper.valueToTree(exportedObj);
	}

	@Override
	protected void createSingle(IdpStatistic toCreate)
	{
		dao.create(toCreate);
	}

	@Override
	protected IdpStatistic fromJsonSingle(ObjectNode src)
	{
		try {
			return jsonMapper.treeToValue(src, IdpStatistic.class);
		} catch (JsonProcessingException e) {
			log.error("Failed to deserialize IdpStatistic object:", e);
		}
		return null;
	}
}








