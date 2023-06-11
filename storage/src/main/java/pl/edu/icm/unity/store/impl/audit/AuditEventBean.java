/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.audit;

import java.util.Date;
import java.util.Set;

import pl.edu.icm.unity.store.rdbms.BaseBean;

/**
 * In DB audit event representation.
 *
 * @author R. Ledzinski
 */
class AuditEventBean extends BaseBean
{
	private String type;
	private Date timestamp;
	private Long subjectId;
	private Long subjectEntityId;
	private String subjectName;
	private String subjectEmail;
	private Long initiatorId;
	private Long initiatorEntityId;
	private String initiatorName;
	private String initiatorEmail;
	private String action;
	private Set<String> tags;

	public AuditEventBean(final String name, final byte[] contents, final String type, final Date timestamp, final Long subjectId, final Long initiatorId, final String action)
	{
		super(name, contents);
		this.type = type;
		this.timestamp = timestamp;
		this.subjectId = subjectId;
		this.initiatorId = initiatorId;
		this.action = action;
	}

	public AuditEventBean()
	{
	}

	public String getType()
	{
		return type;
	}

	public void setType(final String type)
	{
		this.type = type;
	}

	public Date getTimestamp()
	{
		return timestamp;
	}

	public void setTimestamp(final Date timestamp)
	{
		this.timestamp = timestamp;
	}

	Long getSubjectId()
	{
		return subjectId;
	}

	void setSubjectId(final Long subjectId)
	{
		this.subjectId = subjectId;
	}

	Long getSubjectEntityId()
	{
		return subjectEntityId;
	}

	void setSubjectEntityId(final Long subjectEntityId)
	{
		this.subjectEntityId = subjectEntityId;
	}

	String getSubjectName()
	{
		return subjectName;
	}

	void setSubjectName(final String subjectName)
	{
		this.subjectName = subjectName;
	}

	String getSubjectEmail()
	{
		return subjectEmail;
	}

	void setSubjectEmail(final String subjectEmail)
	{
		this.subjectEmail = subjectEmail;
	}

	Long getInitiatorId()
	{
		return initiatorId;
	}

	void setInitiatorId(final Long initiatorId)
	{
		this.initiatorId = initiatorId;
	}

	Long getInitiatorEntityId()
	{
		return initiatorEntityId;
	}

	void setInitiatorEntityId(final Long initiatorEntityId)
	{
		this.initiatorEntityId = initiatorEntityId;
	}

	String getInitiatorName()
	{
		return initiatorName;
	}

	void setInitiatorName(final String initiatorName)
	{
		this.initiatorName = initiatorName;
	}

	String getInitiatorEmail()
	{
		return initiatorEmail;
	}

	void setInitiatorEmail(final String initiatorEmail)
	{
		this.initiatorEmail = initiatorEmail;
	}

	public String getAction()
	{
		return action;
	}

	public void setAction(final String action)
	{
		this.action = action;
	}

	public Set<String> getTags()
	{
		return tags;
	}

	public void setTags(final Set<String> tags)
	{
		this.tags = tags;
	}
}
