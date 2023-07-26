/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.maintenance.audit_log;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.*;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.audit.AuditEventAction;
import pl.edu.icm.unity.base.audit.AuditEventType;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AuditEventManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.identity.UnknownIdentityException;

import javax.annotation.security.PermitAll;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.vaadin.flow.component.icon.VaadinIcon.SEARCH;
import static java.lang.String.join;
import static java.util.Objects.nonNull;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.maintenance.auditLog")
@Route(value = "/audit-log", layout = ConsoleMenu.class)
public class AuditLogView extends ConsoleViewComponent
{
	private final static Logger log = Log.getLogger(Log.U_SERVER_WEB, AuditLogView.class);
	private final static int DEFAULT_LIMIT = 10000;
	private final static String TIMESTAMP = "timestamp";
	private final static DatePicker.DatePickerI18n DATETIME_FORMAT_SHORT_PATTERN = new DatePicker.DatePickerI18n();
	static
	{
		DATETIME_FORMAT_SHORT_PATTERN.setDateFormat("yyyy-MM-dd");
	}

	private final MessageSource msg;
	private final AuditEventManagement eventManagement;
	private final EntityManagement entityMan;
	private final EntityManagement idsMan;
	private final IdentityFormatter identityFormatter;
	private final NotificationPresenter notificationPresenter;

	private final H4 titleLabel = new H4();
	private final H4 diabledMsg = new H4();
	private final MultiSelectComboBox<String> typeFilter = new MultiSelectComboBox<>();
	private final MultiSelectComboBox<String> actionFilter = new MultiSelectComboBox<>();
	private final MultiSelectComboBox<String> tagsFilter = new MultiSelectComboBox<>();
	private final ComboBox<Integer> limitFilter = new ComboBox<>();
	private final TextField searchFilter = new TextField();
	private final DateTimePicker fromFilter = new DateTimePicker();
	private final DateTimePicker untilFilter = new DateTimePicker();

	private Grid<AuditEventEntry> auditEventsGrid;

	AuditLogView(MessageSource msg, AuditEventManagement eventManagement, EntityManagement entityMan,
				 EntityManagement idsMan, NotificationPresenter notificationPresenter, IdentityFormatter identityFormatter)
	{
		this.msg = msg;
		this.eventManagement = eventManagement;
		this.entityMan = entityMan;
		this.idsMan = idsMan;
		this.identityFormatter = identityFormatter;
		this.notificationPresenter = notificationPresenter;
		enter();
	}

	public void enter()
	{
		auditEventsGrid = new Grid<>();
		auditEventsGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

		initGridColumns();
		VerticalLayout filterLayout = createFiltersLayout();

		titleLabel.addClassName("u-AuditEventsGridTitle");

		diabledMsg.addClassName("u-AuditEventsWarnMsg");
		diabledMsg.setText(msg.getMessage("AuditEventsView.disabledMsg"));
		diabledMsg.setVisible(!eventManagement.isPublisherEnabled());

		getContent().add(new VerticalLayout(diabledMsg, titleLabel, filterLayout, auditEventsGrid));
	}

	private void initGridColumns()
	{
		ColumnToggleMenu columnToggleMenu = new ColumnToggleMenu();

		Grid.Column<AuditEventEntry> timestampColumn = auditEventsGrid.addComponentColumn(this::createTimestampWithDetailsArrow)
				.setHeader(getTimestampHeaderLabel())
				.setResizable(true)
				.setSortable(true)
				.setAutoWidth(true);
		timestampColumn.setId(TIMESTAMP);

		Grid.Column<AuditEventEntry> typeColumn = auditEventsGrid.addColumn(ae -> ae.getEvent().getType().toString())
				.setHeader(msg.getMessage("AuditEventsView.type"))
				.setResizable(true)
				.setSortable(true)
				.setAutoWidth(true);
		typeColumn.setId("type");
		columnToggleMenu.addColumn(msg.getMessage("AuditEventsView.type"), typeColumn);

		Grid.Column<AuditEventEntry> actionColumn = auditEventsGrid.addColumn(ae -> ae.getEvent().getAction().toString())
				.setHeader(msg.getMessage("AuditEventsView.action"))
				.setResizable(true)
				.setSortable(true)
				.setAutoWidth(true);
		actionColumn.setId("action");
		columnToggleMenu.addColumn(msg.getMessage("AuditEventsView.action"), actionColumn);

		Grid.Column<AuditEventEntry> nameColumn = auditEventsGrid.addColumn(AuditEventEntry::getName)
				.setHeader(msg.getMessage("AuditEventsView.name"))
				.setResizable(true)
				.setSortable(true)
				.setAutoWidth(true);
		nameColumn.setId("name");
		columnToggleMenu.addColumn(msg.getMessage("AuditEventsView.name"), nameColumn);

		Grid.Column<AuditEventEntry> tagsColumn = auditEventsGrid.addColumn(AuditEventEntry::formatTags)
				.setHeader(msg.getMessage("AuditEventsView.tags"))
				.setResizable(true)
				.setSortable(true)
				.setAutoWidth(true);
		tagsColumn.setId("tags");
		columnToggleMenu.addColumn(msg.getMessage("AuditEventsView.tags"), tagsColumn);

		Grid.Column<AuditEventEntry> subjectIdColumn = auditEventsGrid.addColumn(AuditEventEntry::getSubjectId)
				.setHeader(msg.getMessage("AuditEventsView.subjectId"))
				.setResizable(true)
				.setAutoWidth(true);
		subjectIdColumn.setVisible(false);
		subjectIdColumn.setId("subjectId");
		columnToggleMenu.addColumn(msg.getMessage("AuditEventsView.subjectId"), subjectIdColumn);

		Grid.Column<AuditEventEntry> subjectNameColumn = auditEventsGrid.addColumn(AuditEventEntry::getSubjectName)
				.setHeader(msg.getMessage("AuditEventsView.subjectName"))
				.setResizable(true)
				.setAutoWidth(true);
		subjectNameColumn.setVisible(false);
		subjectNameColumn.setId("subjectName");
		columnToggleMenu.addColumn(msg.getMessage("AuditEventsView.subjectName"), subjectNameColumn);

		Grid.Column<AuditEventEntry> subjectEmailColumn = auditEventsGrid.addColumn(AuditEventEntry::getSubjectEmail)
				.setHeader(msg.getMessage("AuditEventsView.subjectEmail"))
				.setResizable(true)
				.setAutoWidth(true);
		subjectEmailColumn.setVisible(false);
		subjectEmailColumn.setId("subjectEmail");
		columnToggleMenu.addColumn(msg.getMessage("AuditEventsView.subjectEmail"), subjectEmailColumn);

		Grid.Column<AuditEventEntry> initiatorIdColumn = auditEventsGrid.addColumn(AuditEventEntry::getInitiatorId)
				.setHeader(msg.getMessage("AuditEventsView.initiatorId"))
				.setResizable(true)
				.setAutoWidth(true);
		initiatorIdColumn.setVisible(false);
		initiatorIdColumn.setId("initiatorId");
		columnToggleMenu.addColumn(msg.getMessage("AuditEventsView.initiatorId"), initiatorIdColumn);

		Grid.Column<AuditEventEntry> initiatorNameColumn = auditEventsGrid.addColumn(AuditEventEntry::getInitiatorName)
				.setHeader(msg.getMessage("AuditEventsView.initiatorName"))
				.setResizable(true)
				.setAutoWidth(true);
		initiatorNameColumn.setVisible(false);
		initiatorNameColumn.setId("initiatorName");
		columnToggleMenu.addColumn(msg.getMessage("AuditEventsView.initiatorName"), initiatorNameColumn);

		Grid.Column<AuditEventEntry> initiatorEmailColumn = auditEventsGrid.addColumn(AuditEventEntry::getInitiatorEmail)
				.setHeader(msg.getMessage("AuditEventsView.initiatorEmail"))
				.setResizable(true)
				.setAutoWidth(true);
		initiatorEmailColumn.setVisible(false);
		initiatorEmailColumn.setId("initiatorEmail");
		columnToggleMenu.addColumn(msg.getMessage("AuditEventsView.initiatorEmail"), initiatorEmailColumn);

		auditEventsGrid.addComponentColumn(this::createActionsButton)
				.setHeader(getActions(columnToggleMenu))
				.setTextAlign(ColumnTextAlign.END);

		auditEventsGrid.setItemDetailsRenderer(new ComponentRenderer<>(this::getDetailsComponent));
		auditEventsGrid.setDetailsVisibleOnClick(false);
		auditEventsGrid.addSortListener(event -> {
			if(auditEventsGrid.getSortOrder().iterator().hasNext())
				auditEventsGrid.getSortOrder().iterator().next().getSorted().getId().filter(id -> id.equals(TIMESTAMP))
						.ifPresent(id -> auditEventsGrid.getDataProvider().refreshAll());
		});
		auditEventsGrid.sort(List.of(new GridSortOrder<>(timestampColumn, SortDirection.DESCENDING)));
		auditEventsGrid.setAllRowsVisible(true);
	}

	private Label getTimestampHeaderLabel()
	{
		Label label = new Label(msg.getMessage("AuditEventsView.timestamp"));
		label.getStyle().set("margin-left", "var(--big-margin)");
		return label;
	}

	private HorizontalLayout createTimestampWithDetailsArrow(AuditEventEntry eventEntry)
	{
		Label label = new Label(eventEntry.formatTimestamp());
		Icon openIcon = VaadinIcon.ANGLE_RIGHT.create();
		Icon closeIcon = VaadinIcon.ANGLE_DOWN.create();
		openIcon.setVisible(!auditEventsGrid.isDetailsVisible(eventEntry));
		closeIcon.setVisible(auditEventsGrid.isDetailsVisible(eventEntry));
		openIcon.addClickListener(e -> auditEventsGrid.setDetailsVisible(eventEntry, true));
		closeIcon.addClickListener(e -> auditEventsGrid.setDetailsVisible(eventEntry, false));
		return new HorizontalLayout(openIcon, closeIcon, label);
	}

	private HorizontalLayout getActions(ColumnToggleMenu columnToggleMenu)
	{
		HorizontalLayout horizontalLayout = new HorizontalLayout(new Label(msg.getMessage("actions")), columnToggleMenu.getTarget());
		horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		return horizontalLayout;
	}

	private VerticalLayout createFiltersLayout()
	{
		VerticalLayout filterLayout = new VerticalLayout();
		filterLayout.addClassName("u-auditEvents-filterLayout");
		filterLayout.setWidthFull();
		filterLayout.setPadding(false);

		LocalDateTime initialDate = LocalDateTime.now().minusDays(1);
		fromFilter.setValue(initialDate);
		fromFilter.setDatePickerI18n(DATETIME_FORMAT_SHORT_PATTERN);
		fromFilter.setLocale(msg.getLocaleForTimeFormat());
		fromFilter.setLabel(msg.getMessage("AuditEventsView.from"));

		untilFilter.setDatePickerI18n(DATETIME_FORMAT_SHORT_PATTERN);
		untilFilter.setLocale(msg.getLocaleForTimeFormat());
		untilFilter.setLabel(msg.getMessage("AuditEventsView.until"));

		typeFilter.setItems(Arrays.stream(AuditEventType.values()).map(AuditEventType::toString).collect(Collectors.toList()));
		typeFilter.setLabel(msg.getMessage("AuditEventsView.type"));

		actionFilter.setItems(Arrays.stream(AuditEventAction.values()).map(AuditEventAction::toString).collect(Collectors.toList()));
		actionFilter.setLabel(msg.getMessage("AuditEventsView.action"));

		searchFilter.setLabel(msg.getMessage("search"));
		searchFilter.setValueChangeMode(ValueChangeMode.EAGER);

		tagsFilter.setItems(eventManagement.getAllTags().stream().sorted().collect(Collectors.toList()));
		tagsFilter.setLabel(msg.getMessage("AuditEventsView.tags"));

		limitFilter.setItems(100, 1000, 10000);
		limitFilter.setValue(DEFAULT_LIMIT);
		limitFilter.setLabel(msg.getMessage("AuditEventsView.limit"));

		filterLayout.add(new HorizontalLayout(limitFilter, fromFilter, untilFilter), new HorizontalLayout(typeFilter, actionFilter, tagsFilter, searchFilter));

		GridListDataView<AuditEventEntry> auditEventEntryGridListDataView = reloadGrid();
		AuditLogFilter auditLogFilter = new AuditLogFilter(auditEventEntryGridListDataView);

		limitFilter.addValueChangeListener(event ->
		{
			GridListDataView<AuditEventEntry> dataView = reloadGrid();
			auditLogFilter.setDataView(dataView);
		});
		fromFilter.addValueChangeListener(event ->
		{
			GridListDataView<AuditEventEntry> dataView = reloadGrid();
			auditLogFilter.setDataView(dataView);
		});
		untilFilter.addValueChangeListener(event ->
		{
			GridListDataView<AuditEventEntry> dataView = reloadGrid();
			auditLogFilter.setDataView(dataView);
		});
		typeFilter.addValueChangeListener(event -> auditLogFilter.setTypes(event.getValue()));
		actionFilter.addValueChangeListener(event -> auditLogFilter.setActions(event.getValue()));
		searchFilter.addValueChangeListener(event -> auditLogFilter.setSearch(event.getValue()));
		tagsFilter.addValueChangeListener(event -> auditLogFilter.setTags(event.getValue()));

		return filterLayout;
	}

	GridListDataView<AuditEventEntry> reloadGrid()
	{
		Collection<AuditEventEntry> auditEvents = getAuditEvents();
		GridListDataView<AuditEventEntry> auditEventEntryGridListDataView = auditEventsGrid.setItems(auditEvents);
		auditEventEntryGridListDataView.addItemCountChangeListener(event ->
				titleLabel.setText(msg.getMessage("AuditEventsView.gridSummary", event.getItemCount(), auditEvents.size())));
		return auditEventEntryGridListDataView;
	}
	private FormLayout getDetailsComponent(AuditEventEntry ae)
	{
		FormLayout wrapper = new FormLayout();
		wrapper.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		wrapper.addFormItem(new Label(ae.getFormattedSubject()), new FormLayoutLabel(msg.getMessage("AuditEventsView.subject") + ":"));
		wrapper.addFormItem(new Label(ae.getFormattedInitiator()), new FormLayoutLabel(msg.getMessage("AuditEventsView.initiator") + ":"));

		if (!"".equals(ae.formatDetails()))
			wrapper.addFormItem(new Label(ae.formatDetails()), new FormLayoutLabel(msg.getMessage("AuditEventsView.details") + ":"));

		return wrapper;
	}

	private Collection<AuditEventEntry> getAuditEvents()
	{
		try
		{
			Date from = null;
			Date until = null;
			if (nonNull(fromFilter.getValue())) {
				from = Date.from(fromFilter.getValue().atZone(ZoneId.systemDefault()).toInstant());
			}
			if (nonNull(untilFilter.getValue())) {
				until = Date.from(untilFilter.getValue().atZone(ZoneId.systemDefault()).toInstant());
			}
			long now = System.currentTimeMillis();
			SortDirection direction =  auditEventsGrid.getSortOrder().get(0).getSorted().getId().filter(id -> id.equals(TIMESTAMP)).isPresent()
					? auditEventsGrid.getSortOrder().get(0).getDirection()
					: SortDirection.DESCENDING;
			List<AuditEventEntry> list = eventManagement.getAuditEvents(from,
							until,
							limitFilter.getValue(),
							TIMESTAMP,
							direction == SortDirection.ASCENDING ? 1 : -1).stream()
					.map(ae -> new AuditEventEntry(msg, ae))
					.collect(Collectors.toList());
			log.debug("AuditEvents retrieval time: {} ms" , System.currentTimeMillis() - now);
			titleLabel.setText(msg.getMessage("AuditEventsView.gridSummary", list.size(), list.size()));
			return list;
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("Error"), e.getMessage());
		}

		return Collections.emptyList();
	}

	private Component createActionsButton(AuditEventEntry eventEntry)
	{
		ActionMenu actionMenu = new ActionMenu();

		MenuButton showSubjectDetailsButton = new MenuButton(msg.getMessage("AuditEventsView.showDetails", msg.getMessage("AuditEventsView.subject")), SEARCH);
		MenuItem showSubjectDetailsMenuItem = actionMenu.addItem(showSubjectDetailsButton, e -> showSubjectDetails(eventEntry));
		if(eventEntry.getEvent().getSubject() == null)
			showSubjectDetailsMenuItem.setEnabled(false);

		MenuButton showInitiatorDetailsButton = new MenuButton(msg.getMessage("AuditEventsView.showDetails", msg.getMessage("AuditEventsView.initiator")), SEARCH);
		MenuItem showInitiatorDetailsMenuItem = actionMenu.addItem(showInitiatorDetailsButton, e -> showInitiatorDetails(eventEntry));
		if(eventEntry.getEvent().getInitiator().getEntityId() == 0)
			showInitiatorDetailsMenuItem.setEnabled(false);

		Component target = actionMenu.getTarget();
		target.getElement().getStyle().set("margin-right", "var(--big-margin)");
		return target;
	}

	private void showSubjectDetails(AuditEventEntry selection)
	{
		showEntityDetails(selection.getEvent().getSubject().getEntityId());
	}

	private void showInitiatorDetails(AuditEventEntry selection)
	{
		showEntityDetails(selection.getEvent().getInitiator().getEntityId());
	}

	private void showEntityDetails(Long entityId)
	{
		EntityParam param = new EntityParam(entityId);
		Collection<GroupMembership> groups;
		Entity entity;
		String label;
		try
		{
			entity = idsMan.getEntity(param);
			label = idsMan.getEntityLabel(param);
			groups = entityMan.getGroups(new EntityParam(entityId)).values();
		} catch (UnknownIdentityException e) {
			notificationPresenter.showError("Not found", join(" ", "Cannot display entity details.\nEntity",
					Long.toString(entityId), "was removed from the system."));
			return;
		} catch (EngineException e)
		{
			notificationPresenter.showError("Error", "Cannot display entity details.");
			return;
		}
		FormLayout formLayout = EntityDetailsPanelFactory.create(msg, identityFormatter, entity, label, groups);
		new EntityDetailsDialog(formLayout, msg.getMessage("close")).open();
	}

}
