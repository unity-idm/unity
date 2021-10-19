/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.maintenance.idpStatistic;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Layout;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.maintenance.MaintenanceNavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.engine.api.utils.TimeUtil;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

@PrototypeComponent
public class IdpStatisticsView extends CustomComponent implements UnityView
{
	public final static String VIEW_NAME = "IdpStatistics";
	private final static String DATETIME_FORMAT_SHORT_PATTERN = "yyyy-MM-dd HH:mm";
	
	private final MessageSource msg;
	private final IdpStatisticsController idpStatisticController;

	private GridWithActionColumn<GroupedIdpStatistic> statisticGrid;
	private final DateTimeField since;

	public IdpStatisticsView(MessageSource msg, IdpStatisticsController idpStatisticController)
	{
		this.msg = msg;
		this.idpStatisticController = idpStatisticController;
		this.since = new DateTimeField(msg.getMessage("IdpStatisticsView.since"));
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		statisticGrid = new GridWithActionColumn<>(msg, Collections.emptyList(), false, false);
		statisticGrid.setActionColumnHidden(true);

		statisticGrid.addSortableColumn(e -> e.idpId, msg.getMessage("IdpStatisticsView.idpEndpointId"), 10)
				.setResizable(true).setHidable(true);
		statisticGrid.addSortableColumn(e -> e.idpName, msg.getMessage("IdpStatisticsView.idpEndpointName"), 10)
				.setResizable(true);
		statisticGrid.addSortableColumn(e -> e.clientId, msg.getMessage("IdpStatisticsView.clientId"), 10)
				.setResizable(true).setHidable(true);
		statisticGrid.addSortableColumn(e -> e.clientName, msg.getMessage("IdpStatisticsView.clientName"), 10)
				.setResizable(true);
		statisticGrid.addSortableColumn(e -> String.valueOf(e.sigInStats.get(0).successfullCount),
				msg.getMessage("IdpStatisticsView.success"), 10).setResizable(true);
		statisticGrid.addSortableColumn(e -> String.valueOf(e.sigInStats.get(0).failedCount),
				msg.getMessage("IdpStatisticsView.failed"), 10).setResizable(true);

		Layout filterLayout = initButtonsBar();

		VerticalLayout gridWrapper = new VerticalLayout();
		gridWrapper.setMargin(false);
		gridWrapper.setSpacing(false);
		gridWrapper.addComponents(filterLayout, statisticGrid);
		gridWrapper.setExpandRatio(statisticGrid, 2);
		gridWrapper.setSizeFull();
		since.addValueChangeListener(e -> refresh());
		statisticGrid.setSizeFull();
		setCompositionRoot(gridWrapper);
		setSizeFull();
		refresh();
	}

	private Layout initButtonsBar()
	{
		HorizontalLayout buttonsBar = new HorizontalLayout();
		buttonsBar.setMargin(new MarginInfo(true, false));
		LocalDateTime initialDate = LocalDateTime.now().minusDays(30);
		since.setValue(initialDate);
		since.setDateFormat(DATETIME_FORMAT_SHORT_PATTERN);

		FormLayout sinceWrapper = new FormLayout();
		sinceWrapper.addComponent(since);
		sinceWrapper.setMargin(false);
		buttonsBar.addComponent(sinceWrapper);

		Button remove = new Button();
		remove.setCaption(msg.getMessage("IdpStatisticsView.dropOlder"));
		remove.addStyleName("u-button-action");
		remove.setIcon(Images.remove.getResource());
		remove.addClickListener(e -> dropOlderStats());
		buttonsBar.addComponent(remove);

		return buttonsBar;
	}

	private void dropOlderStats()
	{
		new ConfirmDialog(msg,
				msg.getMessage("IdpStatisticsView.dropOlderConfirm",
						TimeUtil.formatStandardInstant(since.getValue().atZone(ZoneId.systemDefault()).toInstant())),
				() ->
				{
					remove();
					refresh();
				}).show();
	}

	private void remove()
	{
		try
		{
			idpStatisticController.drop(since.getValue());

		} catch (ControllerException er)
		{
			NotificationPopup.showError(er);
		}
	}

	public void refresh()
	{

		try
		{
			statisticGrid.setItems(idpStatisticController
					.getIdpStatistics(since.getValue()));
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.maintenance.idpStatistics");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class IdpStatisticsInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		@Autowired
		public IdpStatisticsInfoProvider(MessageSource msg, ObjectFactory<IdpStatisticsView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(MaintenanceNavigationInfoProvider.ID).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.maintenance.idpStatistics"))
					.withIcon(Images.records.getResource()).withPosition(20).build());
		}
	}

}
