/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.audit;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.audit.AuditEntity;
import pl.edu.icm.unity.base.audit.AuditEvent;
import pl.edu.icm.unity.base.audit.AuditEventAction;
import pl.edu.icm.unity.base.audit.AuditEventTag;
import pl.edu.icm.unity.base.audit.AuditEventType;
import pl.edu.icm.unity.base.event.Event;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

/**
 * Holds information required to create AuditEvent object.
 *
 * @author R. Ledzinski
 */
public class AuditEventTrigger implements Event
{
	private AuditEventType type;
	private AuditEventAction action;
	private Date timestamp;
	private String name;
	private Long subjectEntityID;
	private AuditEntity subjectEntity;
	private Long initiatorEntityID;
	private AuditEntity initiatorEntity;
	private ObjectNode details;
	private String[] tags;

	private AuditEventTrigger()
	{
		this.timestamp = new Date();
	}

	public AuditEventType getType()
	{
		return type;
	}

	public AuditEventAction getAction()
	{
		return action;
	}

	public String getName()
	{
		return name;
	}

	Long getSubjectEntityID()
	{
		return subjectEntityID;
	}

	Optional<AuditEntity> getSubjectEntity()
	{
		return isNull(subjectEntity) ? Optional.empty() : Optional.of(subjectEntity);
	}

	Long getInitiatorEntityID()
	{
		return initiatorEntityID;
	}

	Optional<AuditEntity> getInitiatorEntity()
	{
		return isNull(initiatorEntity) ? Optional.empty() : Optional.of(initiatorEntity);
	}

	public ObjectNode getDetails()
	{
		return details;
	}

	public String[] getTags()
	{
		return tags;
	}

	Date getTimestamp()
	{
		return timestamp;
	}

	public static AuditEventTriggerBuilder builder()
	{
		return new AuditEventTriggerBuilder();
	}

	public static class AuditEventTriggerBuilder
	{
		private final static String EMPTY_STRING = "";
		private AuditEventTrigger auditEvent = new AuditEventTrigger();

		public AuditEventTriggerBuilder name(final String name)
		{
			auditEvent.name = name;
			return this;
		}

		public AuditEventTriggerBuilder emptyName()
		{
			auditEvent.name = EMPTY_STRING;
			return this;
		}

		public AuditEventTriggerBuilder type(final AuditEventType type)
		{
			auditEvent.type = type;
			return this;
		}

		public AuditEventTriggerBuilder timestamp(final Date timestamp)
		{
			auditEvent.timestamp = timestamp;
			return this;
		}

		public AuditEventTriggerBuilder action(final AuditEventAction action)
		{
			auditEvent.action = action;
			return this;
		}

		public AuditEventTriggerBuilder details(final Map<String, String> details)
		{
			auditEvent.details = details == null ? null : Constants.MAPPER.valueToTree(details);
			return this;
		}

		public AuditEventTriggerBuilder subject(final AuditEntity subject)
		{
			auditEvent.subjectEntity = subject;
			return this;
		}

		public AuditEventTriggerBuilder subject(final Long subjectId)
		{
			auditEvent.subjectEntityID = subjectId;
			return this;
		}

		AuditEventTriggerBuilder initiator(final Long initiatorId)
		{
			auditEvent.initiatorEntityID = initiatorId;
			return this;
		}

		AuditEventTriggerBuilder initiator(final AuditEntity initiator)
		{
			auditEvent.initiatorEntity = initiator;
			return this;
		}

		public AuditEventTriggerBuilder tags(String... tags)
		{
			auditEvent.tags = tags;
			return this;
		}

		public AuditEventTriggerBuilder tags(AuditEventTag... tags)
		{
			auditEvent.tags = Arrays.stream(tags).map(AuditEventTag::getStringValue).toArray(String[]::new);
			return this;
		}

		public AuditEventTrigger build()
		{
			requireNonNull(auditEvent.name, "AuditEventTrigger.name field is required!");
			requireNonNull(auditEvent.type, "AuditEventTrigger.type field is required!");
			requireNonNull(auditEvent.timestamp, "AuditEventTrigger.timestamp field is required!");
			if (auditEvent.initiatorEntityID == null)
			{
				requireNonNull(auditEvent.initiatorEntity, "AuditEventTrigger.initiator field is required!");
			}
			requireNonNull(auditEvent.action, "AuditEventTrigger.action field is required!");
			// Make sure AuditEvent.name match DB limitation
			if (auditEvent.name.length() > AuditEvent.MAX_NAME_LENGTH)
			{
				auditEvent.name = auditEvent.name.substring(0,  AuditEvent.MAX_NAME_LENGTH - 3) + "...";
			}
			return auditEvent;
		}
	}

	@Override
	public String toString()
	{
		return "AuditEventTrigger{" +
				"type=" + type +
				", action=" + action +
				", name='" + name + '\'' +
				", subjectEntityID=" + subjectEntityID +
				", initiatorEntityID=" + initiatorEntityID +
				", details='" + details + '\'' +
				", tags=" + Arrays.toString(tags) +
				'}';
	}
}
