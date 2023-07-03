/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.audit;

import static java.util.Objects.isNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.audit.AuditEntity;
import pl.edu.icm.unity.base.audit.AuditEvent;
import pl.edu.icm.unity.base.audit.AuditEventAction;
import pl.edu.icm.unity.base.audit.AuditEventType;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;

/**
 * Serializes {@link AuditEvent} to/from DB form.
 * @author R. Ledzinski
 */
@Component
public class AuditEventJsonSerializer implements RDBMSObjectSerializer<AuditEvent, AuditEventBean>
{
	private AuditEntityRDBMSStore auditEntityDAO;

	@Autowired
	public AuditEventJsonSerializer(final AuditEntityRDBMSStore auditEntityDAO) 
	{
		this.auditEntityDAO = auditEntityDAO;
	}

	@Override
	public AuditEventBean toDB(AuditEvent object)
	{
		return new AuditEventBean(
				object.getName(),
				JsonUtil.serialize2Bytes(object.getDetails()),
				object.getType().toString(),
				object.getTimestamp(),
				auditEntityDAO.findOrCreateEntity(object.getSubject()),
				auditEntityDAO.findOrCreateEntity(object.getInitiator()),
				object.getAction().toString());
	}

	@Override
	public AuditEvent fromDB(AuditEventBean bean)
	{
		return AuditEvent.builder()
				.name(bean.getName())
				.type(AuditEventType.valueOf(bean.getType()))
				.timestamp(bean.getTimestamp())
				.action(AuditEventAction.valueOf(bean.getAction()))
				.details(JsonUtil.parse(bean.getContents()))
				.subject(isNull(bean.getSubjectId()) ? null : new AuditEntity(bean.getSubjectEntityId(), bean.getSubjectName(), bean.getSubjectEmail()))
				.initiator(isNull(bean.getInitiatorId()) ? null : new AuditEntity(bean.getInitiatorEntityId(), bean.getInitiatorName(), bean.getInitiatorEmail()))
				.tags(bean.getTags())
				.build();
	}
}
