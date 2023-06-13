/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.maintenance.audit;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import pl.edu.icm.unity.base.audit.AuditEntity;
import pl.edu.icm.unity.base.audit.AuditEvent;
import pl.edu.icm.unity.base.audit.AuditEventType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.grid.FilterableEntry;

/**
 * Represent grid AuditEvent entry.
 * 
 * @author R.Ledzinski
 *
 */
class AuditEventEntry implements FilterableEntry
{
	final private SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	final private List<AuditEventType> USERS_LOG = Arrays.asList(AuditEventType.ENTITY, AuditEventType.IDENTITY);

	private AuditEvent auditEvent;
	private MessageSource msg;
	private String formattedName;
	private String formattedSubject;
	private String formattedInitiator;

	AuditEventEntry(MessageSource msg, AuditEvent auditEvent)
	{
		this.auditEvent = auditEvent;
		this.msg = msg;
		this.formattedName = formatName();
		this.formattedSubject = formatSubject(msg);
		this.formattedInitiator = formatInitiator(msg);
	}

	AuditEvent getEvent()
	{
		return auditEvent;
	}

	String getName()
	{
		return formattedName;
	}

	String getFormattedSubject()
	{
		return formattedSubject;
	}

	String getFormattedInitiator()
	{
		return formattedInitiator;
	}

	String getSubjectId()
	{
		return getEntityId(auditEvent.getSubject());
	}

	String getSubjectName()
	{
		return getEntityName(auditEvent.getSubject());
	}

	String getSubjectEmail()
	{
		return getEntityEmail(auditEvent.getSubject());
	}

	String getInitiatorId()
	{
		return getEntityId(auditEvent.getInitiator());
	}

	String getInitiatorName()
	{
		return getEntityName(auditEvent.getInitiator());
	}

	String getInitiatorEmail()
	{
		return getEntityEmail(auditEvent.getInitiator());
	}

	private String getEntityId(AuditEntity entity)
	{
		return isNull(entity) ? "" : Long.toString(entity.getEntityId());
	}

	private String getEntityName(AuditEntity entity)
	{
		if (isNull(entity) || isNull(entity.getName()) ) 
			return "";
		return entity.getName();
	}

	private String getEntityEmail(AuditEntity entity)
	{
		if (isNull(entity) || isNull(entity.getEmail()) ) 
			return "";
		return entity.getEmail();
	}

	String formatTags() 
	{
		if (auditEvent.getTags().isEmpty())
			return "-";
		return auditEvent.getTags().stream().sorted(Comparator.naturalOrder()).collect(Collectors.joining(", "));
	}

	private String formatName() 
	{
		if (isNull(auditEvent.getSubject()) || !USERS_LOG.contains(auditEvent.getType())) 
		{
			return auditEvent.getName();
		}
		return msg.getMessage("AuditEventsView.nameFormat", auditEvent.getName(), auditEvent.getSubject().getEntityId());
	}

	String formatTimestamp() 
	{
		return DATETIME_FORMAT.format(auditEvent.getTimestamp());
	}

	private String formatSubject(MessageSource msg) 
	{
		return formatEntity(msg, auditEvent.getSubject());
	}

	private String formatInitiator(MessageSource msg) 
	{
		return formatEntity(msg, auditEvent.getInitiator());
	}

	String formatDetails() 
	{
		if (auditEvent.getDetails() == null || auditEvent.getDetails().size() == 0)
			return "";
		
		StringBuilder formatted = new StringBuilder();
		Iterator<Entry<String, JsonNode>> fields = auditEvent.getDetails().fields();
		fields.forEachRemaining(field ->
		{
			formatted.append(field.getKey()).append(": ").append(field.getValue().toString()).append(", ");
		});
		return formatted.toString().substring(0, formatted.length()-2);
	}

	private String formatEntity(MessageSource msg, AuditEntity entity) 
	{
		if (isNull(entity))
			return "";
		return msg.getMessage("AuditEventsView.entityFormat",
				nonNull(entity.getName()) ? entity.getName() : "",
				entity.getEntityId(),
				nonNull(entity.getEmail()) ? entity.getEmail() : "-");
	}

	@Override
	public boolean anyFieldContains(String searched, MessageSource msg)
	{
		boolean isMatching = formattedName.toLowerCase().contains(searched.toLowerCase());

		isMatching = isMatching || formattedSubject.toLowerCase().contains(searched.toLowerCase());

		if (nonNull(auditEvent.getDetails()))
		{
			isMatching = isMatching || auditEvent.getDetails().toString().toLowerCase().contains(searched.toLowerCase());
		}

		return isMatching;
	}
}
