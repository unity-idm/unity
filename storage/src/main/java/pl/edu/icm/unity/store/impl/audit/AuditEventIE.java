/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.api.AuditEventDAO;
import pl.edu.icm.unity.store.export.AbstractIEBase;
import pl.edu.icm.unity.types.basic.audit.AuditEvent;

import java.util.List;

/**
 * Handles import/export of attribute types table.
 *
 * @author R. Ledzinski
 */
@Component
class AuditEventIE extends AbstractIEBase<AuditEvent>
{
	private static final Logger log = Log.getLegacyLogger(Log.U_SERVER_CFG, AuditEventIE.class);
	private AuditEventDAO dao;

	@Autowired
	public AuditEventIE(AuditEventDAO dao)
	{
		super(7, "auditEvents");
		this.dao = dao;
	}

	@Override
	protected List<AuditEvent> getAllToExport()
	{
		return dao.getAll();
	}

	@Override
	protected ObjectNode toJsonSingle(AuditEvent exportedObj)
	{
		return Constants.MAPPER.valueToTree(exportedObj);
	}

	@Override
	protected void createSingle(AuditEvent toCreate)
	{
		dao.create(toCreate);
	}

	@Override
	protected AuditEvent fromJsonSingle(ObjectNode src)
	{
		try {
			return Constants.MAPPER.treeToValue(src, AuditEvent.class);
		} catch (JsonProcessingException e) {
			log.error("Failed to deserialize AuditEvent object:", e);
		}
		return null;
	}
}








