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
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.directorySetup.DirectorySetupNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.helpers.StandardButtonsHelper;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SidebarStyles;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
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

	private UnityMessageSource msg;
	private AutomationController controller;
	private GridWithActionColumn<ScheduledProcessingRule> automationGrid;

	@Autowired
	AutomationView(UnityMessageSource msg, AutomationController controller)
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

		automationGrid.addColumn(r -> r.getCronExpression(),
				msg.getMessage("AutomationView.cronExpressionCaption"), 5);
		automationGrid.addColumn(r -> r.getAction().getName(), msg.getMessage("AutomationView.actionCaption"),
				5);
		automationGrid.addColumn(r -> controller.getActionParamAsString(r.getAction()),
				msg.getMessage("AutomationView.parametersCaption"), 20);

		automationGrid.addHamburgerActions(getHamburgerActionsHandlers());
		automationGrid.setMultiSelect(true);

		automationGrid.setItems(getScheduleRules());

		HamburgerMenu<ScheduledProcessingRule> hamburgerMenu = new HamburgerMenu<>();
		hamburgerMenu.addStyleName(SidebarStyles.sidebar.toString());
		hamburgerMenu.addActionHandlers(getBulkHamburgerActionsHandlers());
		automationGrid.addSelectionListener(hamburgerMenu.getSelectionListener());

		VerticalLayout gridWrapper = new VerticalLayout();
		gridWrapper.setMargin(false);
		gridWrapper.setSpacing(false);
		gridWrapper.addComponent(hamburgerMenu);
		gridWrapper.addComponent(automationGrid);

		VerticalLayout main = new VerticalLayout();
		main.addComponent(buttonsBar);
		main.addComponent(gridWrapper);
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

		@Autowired
		public AutomationNavigationInfoProvider(UnityMessageSource msg,
				DirectorySetupNavigationInfoProvider parent, ObjectFactory<AutomationView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.directorySetup.automation"))
					.withPosition(40).build());

		}
	}
}
