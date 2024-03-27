/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_setup.automation;

import static com.vaadin.flow.component.icon.VaadinIcon.PLAY;
import static com.vaadin.flow.component.icon.VaadinIcon.PLUS_CIRCLE_O;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.grid.ActionMenuWithHandlerSupport;
import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.ComponentWithToolbar;
import io.imunity.vaadin.endpoint.common.Toolbar;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRuleParam;
import pl.edu.icm.unity.base.message.MessageSource;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.directorySetup.automation", parent = "WebConsoleMenu.directorySetup")
@Route(value = "/automation", layout = ConsoleMenu.class)
public class AutomationView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final AutomationController controller;
	private GridWithActionColumn<ScheduledProcessingRule> automationGrid;

	AutomationView(MessageSource msg, AutomationController controller)
	{
		this.msg = msg;
		this.controller = controller;
		initView();
	}

	public void initView()
	{
		automationGrid = new GridWithActionColumn<ScheduledProcessingRule>(msg::getMessage, getActionsHandlers());
		automationGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
		automationGrid.addHamburgerActions(getHamburgerActionsHandlers());
		automationGrid.addColumn(ScheduledProcessingRuleParam::getCronExpression)
				.setHeader(msg.getMessage("AutomationView.cronExpressionCaption"))
				.setAutoWidth(true);
		automationGrid.addColumn(r -> r.getAction()
				.getName())
				.setHeader(msg.getMessage("AutomationView.actionCaption"))
				.setAutoWidth(true);
		automationGrid.addColumn(r -> controller.getActionParamAsString(r.getAction()))
				.setHeader(msg.getMessage("AutomationView.parametersCaption"))
				.setAutoWidth(true);
		automationGrid.setSelectionMode(Grid.SelectionMode.MULTI);
		automationGrid.setItems(getScheduleRules());

		ActionMenuWithHandlerSupport<ScheduledProcessingRule> hamburgerMenu = new ActionMenuWithHandlerSupport<>();
		hamburgerMenu.addActionHandlers(getBulkHamburgerActionsHandlers());
		automationGrid.addSelectionListener(hamburgerMenu.getSelectionListener());

		Toolbar<ScheduledProcessingRule> toolbar = new Toolbar<>();
		toolbar.addHamburger(hamburgerMenu);
		ComponentWithToolbar attrTypeGridWithToolbar = new ComponentWithToolbar(automationGrid, toolbar);
		attrTypeGridWithToolbar.setSpacing(false);
		attrTypeGridWithToolbar.setSizeFull();

		VerticalLayout layout = new VerticalLayout(createHeaderLayout(), attrTypeGridWithToolbar);
		layout.setSpacing(false);
		getContent().add(layout);
	}

	private List<SingleActionHandler<ScheduledProcessingRule>> getActionsHandlers()
	{
		SingleActionHandler<ScheduledProcessingRule> edit = SingleActionHandler
				.builder4Edit(msg::getMessage, ScheduledProcessingRule.class)
				.withHandler(r -> gotoEdit(r.iterator()
						.next()))
				.build();

		return Arrays.asList(edit);
	}

	private List<SingleActionHandler<ScheduledProcessingRule>> getHamburgerActionsHandlers()
	{
		SingleActionHandler<ScheduledProcessingRule> run = SingleActionHandler.builder(ScheduledProcessingRule.class)
				.withCaption(msg.getMessage("AutomationView.runNowAction"))
				.withIcon(PLAY)
				.withHandler(r -> gotoRun(r.iterator()
						.next()))
				.build();

		return Stream.concat(Arrays.asList(run)
				.stream(), getBulkHamburgerActionsHandlers().stream())
				.collect(Collectors.toList());
	}

	private List<SingleActionHandler<ScheduledProcessingRule>> getBulkHamburgerActionsHandlers()
	{
		SingleActionHandler<ScheduledProcessingRule> remove = SingleActionHandler
				.builder4Delete(msg::getMessage, ScheduledProcessingRule.class)
				.withHandler(this::tryRemove)
				.build();

		return Arrays.asList(remove);

	}

	private void gotoEdit(ScheduledProcessingRule rule)
	{

		UI.getCurrent()
				.navigate(AutomationEditView.class, rule.getId());
	}

	private void gotoRun(ScheduledProcessingRule rule)
	{
		UI.getCurrent()
				.navigate(AutomationRunView.class, rule.getId());
	}

	private VerticalLayout createHeaderLayout()
	{
		VerticalLayout headerLayout = new VerticalLayout();
		headerLayout.setPadding(false);
		headerLayout.setSpacing(false);
		Button addButton = new Button(msg.getMessage("addNew"), e -> UI.getCurrent()
				.navigate(AutomationEditView.class));
		addButton.setIcon(PLUS_CIRCLE_O.create());
		Button runButton = new Button(msg.getMessage("AutomationView.runAdhoc"), e -> UI.getCurrent()
				.navigate(AutomationRunView.class));
		runButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		runButton.setIcon(PLAY.create());
		headerLayout.setAlignItems(FlexComponent.Alignment.END);
		HorizontalLayout upperLayout = new HorizontalLayout(addButton, runButton);
		headerLayout.add(upperLayout);
		return headerLayout;
	}

	private Collection<ScheduledProcessingRule> getScheduleRules()
	{
		return controller.getScheduleRules();
	}

	private void tryRemove(Set<ScheduledProcessingRule> items)
	{
		new ConfirmDialog(msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("AutomationView.confirmDelete", items.size()), msg.getMessage("ok"), e -> remove(items),
				msg.getMessage("cancel"), e ->
				{
				}).open();
	}

	private void remove(Set<ScheduledProcessingRule> items)
	{
		controller.removeScheduledRules(items);
		automationGrid.setItems(controller.getScheduleRules());
	}
}
