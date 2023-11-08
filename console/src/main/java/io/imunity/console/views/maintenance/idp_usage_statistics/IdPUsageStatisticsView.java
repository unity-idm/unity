/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.maintenance.idp_usage_statistics;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.ColumnToggleMenu;
import io.imunity.vaadin.elements.FormLayoutLabel;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;

import jakarta.annotation.security.PermitAll;
import java.time.LocalDateTime;
import java.time.ZoneId;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.maintenance.idpStatistics", parent = "WebConsoleMenu.maintenance")
@Route(value = "/idp-usage-statistics", layout = ConsoleMenu.class)
public class IdPUsageStatisticsView extends ConsoleViewComponent
{
	private final static String DATETIME_FORMAT_SHORT_PATTERN = "yyyy-MM-dd";

	private final MessageSource msg;
	private final IdPUsageStatisticsController idpStatisticController;

	private Grid<GroupedIdpStatistic> statisticGrid;
	private final DateTimePicker since;

	IdPUsageStatisticsView(MessageSource msg, IdPUsageStatisticsController idpStatisticController)
	{
		this.msg = msg;
		this.idpStatisticController = idpStatisticController;
		this.since = new DateTimePicker();
		init();
	}

	public void init()
	{
		statisticGrid = new Grid<>();
		Grid.Column<GroupedIdpStatistic> endpointIdColumn = statisticGrid.addColumn(e -> e.idpId)
				.setHeader(msg.getMessage("IdpStatisticsView.idpEndpointId"))
				.setResizable(true)
				.setSortable(true)
				.setAutoWidth(true);
		statisticGrid.addColumn(e -> e.idpName)
				.setHeader(msg.getMessage("IdpStatisticsView.idpEndpointName"))
				.setResizable(true)
				.setAutoWidth(true)
				.setSortable(true);
		Grid.Column<GroupedIdpStatistic> clientIdColumn = statisticGrid.addColumn(e -> e.clientId)
				.setHeader(msg.getMessage("IdpStatisticsView.clientId"))
				.setResizable(true)
				.setAutoWidth(true)
				.setSortable(true);
		statisticGrid.addColumn(e -> e.clientName)
				.setHeader(msg.getMessage("IdpStatisticsView.clientName"))
				.setResizable(true)
				.setAutoWidth(true)
				.setSortable(true);
		statisticGrid.addColumn(e -> String.valueOf(e.sigInStats.get(0).successfullCount))
				.setHeader(msg.getMessage("IdpStatisticsView.success"))
				.setResizable(true)
				.setAutoWidth(true)
				.setSortable(true);
		statisticGrid.addColumn(e -> String.valueOf(e.sigInStats.get(0).failedCount))
				.setHeader(msg.getMessage("IdpStatisticsView.failed"))
				.setResizable(true)
				.setAutoWidth(true)
				.setSortable(true);

		ColumnToggleMenu columnToggleMenu = new ColumnToggleMenu();
		columnToggleMenu.addColumn(msg.getMessage("IdpStatisticsView.idpEndpointId"), endpointIdColumn);
		columnToggleMenu.addColumn(msg.getMessage("IdpStatisticsView.clientId"), clientIdColumn);
		statisticGrid.addColumn(e -> "")
				.setHeader(columnToggleMenu.getTarget())
				.setTextAlign(ColumnTextAlign.END);
		statisticGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

		HorizontalLayout upperLayout = getUpperLayout();

		since.addValueChangeListener(e -> refresh());
		refresh();
		getContent().add(new VerticalLayout(upperLayout, statisticGrid));
	}

	private HorizontalLayout getUpperLayout()
	{
		HorizontalLayout upperLayout = new HorizontalLayout();
		upperLayout.setWidthFull();
		upperLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

		LocalDateTime initialDate = LocalDateTime.now().minusDays(30);
		since.setValue(initialDate);
		DatePicker.DatePickerI18n datePickerI18n = new DatePicker.DatePickerI18n();
		datePickerI18n.setDateFormat(DATETIME_FORMAT_SHORT_PATTERN);
		since.setDatePickerI18n(datePickerI18n);
		since.setLocale(msg.getLocaleForTimeFormat());

		FormLayout sinceWrapper = new FormLayout();
		sinceWrapper.addFormItem(since, new FormLayoutLabel(msg.getMessage("IdpStatisticsView.since")));

		Button remove = new Button();
		remove.setText(msg.getMessage("IdpStatisticsView.dropOlder"));
		remove.setIcon(VaadinIcon.TRASH.create());
		remove.addClickListener(e -> dropOlderStats());

		upperLayout.add(sinceWrapper, remove);

		return upperLayout;
	}

	private void dropOlderStats()
	{
		new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("IdpStatisticsView.dropOlderConfirm", TimeUtil.formatStandardInstant(
						since.getValue().atZone(ZoneId.systemDefault()).toInstant())
				),
				msg.getMessage("ok"), e -> idpStatisticController.drop(since.getValue()),
				msg.getMessage("cancel"), e -> refresh()
		).open();
	}

	public void refresh()
	{
		statisticGrid.setItems(idpStatisticController.getIdpStatistics(since.getValue()));
	}

}
