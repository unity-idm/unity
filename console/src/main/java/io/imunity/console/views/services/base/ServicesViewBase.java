/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.services.base;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Sets;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RouterLink;

import io.imunity.console.views.CommonViewParam;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.console.views.ShowViewActionLayoutFactory;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.api.services.ServiceDefinition;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditorComponent.ServiceEditorTab;
import io.imunity.vaadin.endpoint.common.api.services.ServiceTypeInfoHelper;
import pl.edu.icm.unity.base.endpoint.Endpoint.EndpointState;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;

import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * 
 * @author P.Piernik
 *
 */

public abstract class ServicesViewBase extends ConsoleViewComponent
{
	protected final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private final ServiceControllerBase controller;
	private final Class<? extends ConsoleViewComponent> newServiceViewName;
	private final Class<? extends ConsoleViewComponent> editServiceViewName;
	private GridWithActionColumn<ServiceDefinition> servicesGrid;

	public ServicesViewBase(MessageSource msg, NotificationPresenter notificationPresenter,
			ServiceControllerBase controller, Class<? extends ConsoleViewComponent> newServiceViewName,
			Class<? extends ConsoleViewComponent> editServiceViewName)
	{
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
		this.controller = controller;
		this.newServiceViewName = newServiceViewName;
		this.editServiceViewName = editServiceViewName;
	}

	protected void initUI()
	{

		servicesGrid = new GridWithActionColumn<>(msg::getMessage, getActionsHandlers());
		Column<ServiceDefinition> name = servicesGrid
				.addComponentColumn(e -> new RouterLink(e.getName(), editServiceViewName, e.getName()))
				.setHeader(msg.getMessage("ServicesView.nameCaption"))
				.setAutoWidth(true)
				.setSortable(true)
				.setComparator(Comparator.comparing(r -> r.getName()));
		servicesGrid.addComponentColumn(e -> getStatusLabel(e.getState()))
				.setHeader(msg.getMessage("ServicesView.statusCaption"));
		servicesGrid.addColumn(e -> ServiceTypeInfoHelper.getType(msg, e.getType()))
				.setHeader(msg.getMessage("ServicesView.typeCaption"));
		servicesGrid.addColumn(e -> ServiceTypeInfoHelper.getBinding(msg, e.getBinding()))
				.setHeader(msg.getMessage("ServicesView.bindingCaption"));
		servicesGrid.addHamburgerActions(getHamburgerActionsHandlers());
		servicesGrid.setItems(getServices());
		servicesGrid.sort(List.of(new GridSortOrder<>(name, SortDirection.DESCENDING)));

		List<Button> buttons = getButtonsBar();
		VerticalLayout main = new VerticalLayout();
		main.add(ShowViewActionLayoutFactory.buildTopButtonsBar(buttons.toArray(new Button[buttons.size()])));
		main.add(servicesGrid);
		main.setWidthFull();
		main.setMargin(false);
		getContent().add(main);
		getContent().setSizeFull();

	}

	protected List<Button> getButtonsBar()
	{
		Button newProfile = ShowViewActionLayoutFactory.build4AddAction(msg, e -> UI.getCurrent()
				.navigate(newServiceViewName));
		return Arrays.asList(newProfile);
	}

	private Icon getStatusLabel(EndpointState state)
	{
		Icon icon = new Icon(state.equals(EndpointState.DEPLOYED) ? VaadinIcon.CHECK_CIRCLE_O : VaadinIcon.BAN);
		icon.setTooltipText(state.toString());
		return icon;
	}

	private Collection<ServiceDefinition> getServices()
	{
		try
		{
			return controller.getServices();
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getDetails());
			return Collections.emptyList();
		}
	}

	void refresh()
	{
		Collection<ServiceDefinition> services = getServices();
		servicesGrid.setItems(services);
	}

	protected abstract List<SingleActionHandler<ServiceDefinition>> getActionsHandlers();

	private List<SingleActionHandler<ServiceDefinition>> getHamburgerActionsHandlers()
	{
		SingleActionHandler<ServiceDefinition> remove = SingleActionHandler
				.builder4Delete(msg::getMessage, ServiceDefinition.class)
				.withHandler(r -> tryRemove(r.iterator()
						.next()))
				.build();
		SingleActionHandler<ServiceDefinition> deploy = SingleActionHandler.builder(ServiceDefinition.class)
				.withCaption(msg.getMessage("ServicesView.deploy"))
				.withIcon(VaadinIcon.PLAY)
				.withDisabledPredicate(e -> e.getState()
						.equals(EndpointState.DEPLOYED))
				.withHandler(r -> deploy(r.iterator()
						.next()))
				.build();
		SingleActionHandler<ServiceDefinition> undeploy = SingleActionHandler.builder(ServiceDefinition.class)
				.withCaption(msg.getMessage("ServicesView.undeploy"))
				.withDisabledPredicate(e -> e.getState()
						.equals(EndpointState.UNDEPLOYED))
				.withIcon(VaadinIcon.BAN)
				.withHandler(r -> undeploy(r.iterator()
						.next()))
				.build();
		SingleActionHandler<ServiceDefinition> reload = SingleActionHandler.builder(ServiceDefinition.class)
				.withCaption(msg.getMessage("ServicesView.reload"))
				.withDisabledPredicate(e -> !e.supportsConfigReloadFromFile())
				.withIcon(VaadinIcon.RETWEET)
				.withHandler(r -> reload(r.iterator()
						.next()))
				.build();

		return Arrays.asList(remove, deploy, undeploy, reload);

	}

	private void undeploy(ServiceDefinition endpoint)
	{
		try
		{
			controller.undeploy(endpoint);
			refresh();
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getDetails());
		}
	}

	private void deploy(ServiceDefinition endpoint)
	{
		try
		{
			controller.deploy(endpoint);
			refresh();
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getDetails());
		}
	}

	private void reload(ServiceDefinition endpoint)
	{
		try
		{
			controller.reload(endpoint);
			refresh();
			notificationPresenter.showSuccess(msg.getMessage("ServicesView.reloadSuccess", endpoint.getName()), "");

		} catch (ControllerException e)
		{
			refresh();
			notificationPresenter.showError(e.getCaption(), e.getDetails());
		}
	}

	private void tryRemove(ServiceDefinition endpoint)
	{
		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(endpoint.getName()));
		new ConfirmDialog(msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("ServicesView.confirmDelete", confirmText), msg.getMessage("ok"), e -> remove(endpoint),
				msg.getMessage("cancel"), e ->
				{
				}).open();
	}

	private void remove(ServiceDefinition endpoint)
	{
		try
		{
			controller.remove(endpoint);
			refresh();
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getDetails());
		}

	}

	protected void gotoEdit(ServiceDefinition service, ServiceEditorTab authentication)
	{
		UI.getCurrent()
				.navigate(editServiceViewName, service.getName(),
						new QueryParameters(Map.of(CommonViewParam.tab.name(), List.of(authentication.name()))));
	}
}
