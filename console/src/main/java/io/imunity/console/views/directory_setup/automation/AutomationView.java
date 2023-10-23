/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_setup.automation;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.ActionIconBuilder;
import io.imunity.vaadin.elements.ActionMenu;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.MenuButton;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRuleParam;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.Collection;
import java.util.Set;

import static com.vaadin.flow.component.icon.VaadinIcon.*;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.directorySetup.automation", parent = "WebConsoleMenu.directorySetup")
@Route(value = "/automation", layout = ConsoleMenu.class)
public class AutomationView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final AutomationController controller;
	private Grid<ScheduledProcessingRule> automationGrid;

	AutomationView(MessageSource msg, AutomationController controller)
	{
		this.msg = msg;
		this.controller = controller;
		initView();
	}

	public void initView()
	{
		automationGrid = new Grid<>();
		automationGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		automationGrid.addColumn(ScheduledProcessingRuleParam::getCronExpression)
				.setHeader(msg.getMessage("AutomationView.cronExpressionCaption"))
				.setAutoWidth(true);
		automationGrid.addColumn(r -> r.getAction().getName())
				.setHeader(msg.getMessage("AutomationView.actionCaption"))
				.setAutoWidth(true);
		automationGrid.addColumn(r -> controller.getActionParamAsString(r.getAction()))
				.setHeader(msg.getMessage("AutomationView.parametersCaption"))
				.setAutoWidth(true);
		automationGrid.addComponentColumn(this::createRowActionMenu)
				.setHeader(msg.getMessage("actions"))
				.setTextAlign(ColumnTextAlign.END);
		automationGrid.setSelectionMode(Grid.SelectionMode.MULTI);
		automationGrid.setItems(getScheduleRules());

		VerticalLayout layout = new VerticalLayout(createHeaderLayout(), automationGrid);
		layout.setSpacing(false);
		getContent().add(layout);
	}

	private VerticalLayout createHeaderLayout()
	{
		VerticalLayout headerLayout = new VerticalLayout();
		headerLayout.setPadding(false);
		headerLayout.setSpacing(false);
		Button addButton = new Button(msg.getMessage("addNew"), e -> UI.getCurrent().navigate(AutomationEditView.class));
		addButton.setIcon(PLUS_CIRCLE_O.create());
		Button runButton = new Button(msg.getMessage("AutomationView.runAdhoc"), e -> UI.getCurrent().navigate(AutomationRunView.class));
		runButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		runButton.setIcon(PLAY.create());
		headerLayout.setAlignItems(FlexComponent.Alignment.END);
		HorizontalLayout upperLayout = new HorizontalLayout(addButton, runButton);
		HorizontalLayout lowerLayout = new HorizontalLayout(createMainMenu());
		lowerLayout.setWidthFull();
		headerLayout.add(upperLayout, lowerLayout);
		return headerLayout;
	}

	private Component createMainMenu()
	{
		ActionMenu actionMenu = new ActionMenu();

		MenuButton removeButton = new MenuButton(msg.getMessage("remove"), TRASH);
		removeButton.setEnabled(false);
		actionMenu.addItem(removeButton, e -> tryRemove(automationGrid.getSelectedItems()));

		Component target = actionMenu.getTarget();
		target.getElement().getStyle().set("margin-left", "1.3em");
		automationGrid.addSelectionListener(event -> removeButton.setEnabled(!event.getAllSelectedItems().isEmpty()));
		return target;
	}

	private Component createRowActionMenu(ScheduledProcessingRule entry)
	{
		Icon generalSettings = new ActionIconBuilder()
				.icon(EDIT)
				.tooltipText(msg.getMessage("edit"))
				.navigation(AutomationEditView.class, entry.getName())
				.build();

		ActionMenu actionMenu = new ActionMenu();

		MenuButton previewButton = new MenuButton(msg.getMessage("AutomationView.runNowAction"), PLAY);
		actionMenu.addItem(previewButton, e -> UI.getCurrent().navigate(AutomationRunView.class, entry.getName()));

		MenuButton removeButton = new MenuButton(msg.getMessage("remove"), TRASH);
		actionMenu.addItem(removeButton, e -> tryRemove(Set.of(entry)));

		HorizontalLayout horizontalLayout = new HorizontalLayout(generalSettings, actionMenu.getTarget());
		horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		return horizontalLayout;
	}

	private Collection<ScheduledProcessingRule> getScheduleRules()
	{
		return controller.getScheduleRules();
	}

	private void tryRemove(Set<ScheduledProcessingRule> items)
	{
		new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("AutomationView.confirmDelete", items.size()),
				msg.getMessage("ok"),
				e -> remove(items),
				msg.getMessage("cancel"),
				e -> {}
		).open();
	}

	private void remove(Set<ScheduledProcessingRule> items)
	{
		controller.removeScheduledRules(items);
		automationGrid.setItems(controller.getScheduleRules());
	}
}
