/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.audit;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.audit.AuditEvent;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.api.AuditEventDAO;
import pl.edu.icm.unity.store.export.AbstractIEBase;

/**
 * Handles import/export of attribute types table.
 *
 * @author R. Ledzinski
 */
@Component
public class AuditEventIE extends AbstractIEBase<AuditEvent>
{
	public static final String AUDIT_EVENTS_OBJECT_TYPE = "auditEvents";
	
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, AuditEventIE.class);
	private AuditEventDAO dao;

	@Autowired
	public AuditEventIE(AuditEventDAO dao, ObjectMapper objectMapper)
	{
		super(8, AUDIT_EVENTS_OBJECT_TYPE, objectMapper);
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
		return jsonMapper.valueToTree(exportedObj);
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
			return jsonMapper.treeToValue(src, AuditEvent.class);
		} catch (JsonProcessingException e) {
			log.error("Failed to deserialize AuditEvent object:", e);
		}
		return null;
	}
}








