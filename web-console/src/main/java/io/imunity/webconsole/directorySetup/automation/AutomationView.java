/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directorySetup.automation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.directorySetup.DirectorySetupNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Lists all bulk rules
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class AutomationView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "Automation";

	private MessageSource msg;
	private AutomationController controller;
	private GridWithActionColumn<ScheduledProcessingRule> automationGrid;

	@Autowired
	AutomationView(MessageSource msg, AutomationController controller)
	{
		this.msg = msg;
		this.controller = controller;

	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		HorizontalLayout buttonsBar = StandardButtonsHelper.buildTopButtonsBar(

				StandardButtonsHelper.buildButton(msg.getMessage("addNew"), Images.add,
						e -> NavigationHelper.goToView(NewAutomationView.VIEW_NAME)),

				StandardButtonsHelper.buildActionButton(msg.getMessage("AutomationView.runAdhoc"),
						Images.play,
						e -> NavigationHelper.goToView(RunImmediateView.VIEW_NAME)));

		automationGrid = new GridWithActionColumn<>(msg, getActionsHandlers(), false);

		automationGrid.addSortableColumn(r -> r.getCronExpression(),
				msg.getMessage("AutomationView.cronExpressionCaption"), 5);
		automationGrid.addSortableColumn(r -> r.getAction().getName(), msg.getMessage("AutomationView.actionCaption"),
				5);
		automationGrid.addSortableColumn(r -> controller.getActionParamAsString(r.getAction()),
				msg.getMessage("AutomationView.parametersCaption"), 20);

		automationGrid.addHamburgerActions(getHamburgerActionsHandlers());
		automationGrid.setMultiSelect(true);

		automationGrid.setItems(getScheduleRules());

		HamburgerMenu<ScheduledProcessingRule> hamburgerMenu = new HamburgerMenu<>();
		hamburgerMenu.addActionHandlers(getBulkHamburgerActionsHandlers());
		automationGrid.addSelectionListener(hamburgerMenu.getSelectionListener());

		Toolbar<ScheduledProcessingRule> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		toolbar.setWidth(100, Unit.PERCENTAGE);
		toolbar.addHamburger(hamburgerMenu);
		ComponentWithToolbar automationGridWithToolbar = new ComponentWithToolbar(automationGrid, toolbar, Alignment.BOTTOM_LEFT);
		automationGridWithToolbar.setSizeFull();
		automationGridWithToolbar.setSpacing(false);

		VerticalLayout main = new VerticalLayout();
		main.addComponent(buttonsBar);
		main.addComponent(automationGridWithToolbar);
		main.setWidth(100, Unit.PERCENTAGE);
		main.setMargin(false);

		setCompositionRoot(main);
	}

	private Collection<ScheduledProcessingRule> getScheduleRules()
	{
		try
		{
			return controller.getScheduleRules();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
		return Collections.emptyList();
	}

	private List<SingleActionHandler<ScheduledProcessingRule>> getActionsHandlers()
	{
		SingleActionHandler<ScheduledProcessingRule> edit = SingleActionHandler
				.builder4Edit(msg, ScheduledProcessingRule.class)
				.withHandler(r -> gotoEdit(r.iterator().next())).build();

		return Arrays.asList(edit);
	}

	private List<SingleActionHandler<ScheduledProcessingRule>> getHamburgerActionsHandlers()
	{
		SingleActionHandler<ScheduledProcessingRule> run = SingleActionHandler
				.builder(ScheduledProcessingRule.class)
				.withCaption(msg.getMessage("AutomationView.runNowAction"))
				.withIcon(Images.play.getResource()).withHandler(r -> gotoRun(r.iterator().next()))
				.build();

		return Stream.concat(Arrays.asList(run).stream(), getBulkHamburgerActionsHandlers().stream())
				.collect(Collectors.toList());
	}

	private List<SingleActionHandler<ScheduledProcessingRule>> getBulkHamburgerActionsHandlers()
	{
		SingleActionHandler<ScheduledProcessingRule> remove = SingleActionHandler
				.builder4Delete(msg, ScheduledProcessingRule.class).withHandler(this::tryRemove)
				.build();

		return Arrays.asList(remove);

	}

	private void tryRemove(Set<ScheduledProcessingRule> items)
	{
		ConfirmDialog confirm = new ConfirmDialog(msg,
				msg.getMessage("AutomationView.confirmDelete", items.size()), () -> {
					remove(items);
				});
		confirm.show();
	}

	private void remove(Set<ScheduledProcessingRule> items)
	{
		try
		{
			controller.removeScheduledRules(items);
			items.forEach(m -> automationGrid.removeElement(m));
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
	}

	private void gotoEdit(ScheduledProcessingRule rule)
	{
		NavigationHelper.goToView(EditAutomationView.VIEW_NAME + "/" + CommonViewParam.id.toString() + "="
				+ rule.getId());
	}

	private void gotoRun(ScheduledProcessingRule rule)
	{
		NavigationHelper.goToView(RunImmediateView.VIEW_NAME + "/" + CommonViewParam.id.toString() + "="
				+ rule.getId());
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.directorySetup.automation");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class AutomationNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		public static final String ID = VIEW_NAME;
		
		@Autowired
		public AutomationNavigationInfoProvider(MessageSource msg, ObjectFactory<AutomationView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(DirectorySetupNavigationInfoProvider.ID).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.directorySetup.automation"))
					.withIcon(Images.calendar_user.getResource())
					.withPosition(40).build());

		}
	}
}
