/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.maintenance.audit_log;

import com.vaadin.flow.component.grid.dataview.GridListDataView;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

class AuditLogFilter
{
	private GridListDataView<AuditEventEntry> dataView;

	private LocalDateTime from;
	private LocalDateTime until;
	private Set<String> types;
	private Set<String> tags;
	private Set<String> actions;
	private String search;

	public AuditLogFilter(GridListDataView<AuditEventEntry> dataView)
	{
		this.dataView = dataView;
		this.dataView.addFilter(this::test);
	}

	public void setDataView(GridListDataView<AuditEventEntry> dataView)
	{
		this.dataView = dataView;
		this.dataView.addFilter(this::test);
		this.dataView.refreshAll();
	}

	public void setFrom(LocalDateTime from)
	{
		this.from = from;
		this.dataView.refreshAll();
	}

	public void setUntil(LocalDateTime until)
	{
		this.until = until;
		this.dataView.refreshAll();
	}

	public void setTypes(Set<String> types)
	{
		this.types = Set.copyOf(types);
		this.dataView.refreshAll();
	}

	public void setTags(Set<String> tags)
	{
		this.tags = Set.copyOf(tags);
		this.dataView.refreshAll();
	}

	public void setActions(Set<String> actions)
	{
		this.actions = Set.copyOf(actions);
		this.dataView.refreshAll();
	}

	public void setSearch(String search)
	{
		this.search = search;
		this.dataView.refreshAll();
	}

	private boolean test(AuditEventEntry auditEventEntry)
	{
		ZoneOffset offset = ZonedDateTime.now().getOffset();
		boolean matchesFrom = Optional.ofNullable(from)
				.map(time -> time.toInstant(offset).isAfter(auditEventEntry.getEvent().getTimestamp().toInstant()))
				.isEmpty();
		boolean matchesUntil = Optional.ofNullable(until)
				.map(time -> time.toInstant(offset).isBefore(auditEventEntry.getEvent().getTimestamp().toInstant()))
				.isEmpty();
		boolean matchesTags = matches(auditEventEntry.formatTags(), tags);
		boolean matchesTypes = matches(auditEventEntry.getEvent().getType().name(), types);
		boolean matchesActions = matches(auditEventEntry.getEvent().getAction().name(), actions);
		boolean matchesSearch = auditEventEntry.anyFieldContains(search);

		return matchesFrom && matchesUntil && matchesTags && matchesTypes && matchesActions && matchesSearch;
	}

	private boolean matches(String value, Set<String> searchTerm)
	{
		return searchTerm == null || searchTerm.isEmpty()
				|| searchTerm.contains(value);
	}

}
