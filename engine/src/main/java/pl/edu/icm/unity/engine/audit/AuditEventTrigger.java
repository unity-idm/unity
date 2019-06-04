/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.audit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.AbstractEvent;
import pl.edu.icm.unity.types.basic.audit.AuditEntity;
import pl.edu.icm.unity.types.basic.audit.EventAction;
import pl.edu.icm.unity.types.basic.audit.EventType;

import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

/**
 * Holds information required to create AuditEvent object.
 *
 * @author R. Ledzinski
 */
public class AuditEventTrigger extends AbstractEvent
{
	private EventType type;
	private EventAction action;
	private Date timestamp;
	private String name;
	private Long subjectEntityID;
	private AuditEntity subjectEntity;
	private long initiatorEntityID;
	private ObjectNode details;
	private String[] tags;

	public AuditEventTrigger(final EventType type, final EventAction action, final String name,
							 final Long subjectEntityID, final long initiatorEntityID, final Map<String, String> details,
							 final String ... tags)
	{

		this.type = type;
		this.action = action;
		this.name = name;
		this.subjectEntityID = subjectEntityID;
		this.initiatorEntityID = initiatorEntityID;
		this.details = details == null ? null : Constants.MAPPER.valueToTree(details);
		this.tags = tags;
		this.timestamp = new Date();
	}

	public AuditEventTrigger(final EventType type, final EventAction action, final String name,
							 final AuditEntity subjectEntity, final long initiatorEntityID, final Map<String, String> details,
							 final String ... tags)
	{
		this.type = type;
		this.action = action;
		this.name = name;
		this.subjectEntity = subjectEntity;
		this.initiatorEntityID = initiatorEntityID;
		this.details = details == null ? null : Constants.MAPPER.valueToTree(details);
		this.tags = tags;
		this.timestamp = new Date();
	}

	public EventType getType()
	{
		return type;
	}

	public EventAction getAction()
	{
		return action;
	}

	public String getName()
	{
		return name;
	}

	public Long getSubjectEntityID()
	{
		return subjectEntityID;
	}

	public Optional<AuditEntity> getSubjectEntity()
	{
		return isNull(subjectEntity) ? Optional.empty() : Optional.of(subjectEntity);
	}

	public long getInitiatorEntityID()
	{
		return initiatorEntityID;
	}

	public ObjectNode getDetails()
	{
		return details;
	}

	public String[] getTags()
	{
		return tags;
	}

	public Date getTimestamp()
	{
		return timestamp;
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
