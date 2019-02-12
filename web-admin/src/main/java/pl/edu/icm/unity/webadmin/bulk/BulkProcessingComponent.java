/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.bulk;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.BulkProcessingManagement;
import pl.edu.icm.unity.engine.api.bulkops.EntityActionsRegistry;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.types.bulkops.ScheduledProcessingRuleParam;
import pl.edu.icm.unity.types.translation.TranslationRule;
import pl.edu.icm.unity.webadmin.tprofile.ActionEditor;
import pl.edu.icm.unity.webadmin.tprofile.ActionParameterComponentProvider;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;

/**
 * Component responsible for management of bulk processing actions. 
 * Contains two parts: button for launching single shot rule and UI to present and manage scheduled rules.
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BulkProcessingComponent extends CustomComponent
{
	private UnityMessageSource msg;
	private BulkProcessingManagement bulkManagement;
	private EntityActionsRegistry registry;
	private ActionParameterComponentProvider parameterFactory;

	private GenericElementsTable<ScheduledProcessingRule> table;
	private ScheduledRuleViewerPanel viewer;
	private VerticalLayout main;
	
	@Autowired
	public BulkProcessingComponent(UnityMessageSource msg,
			BulkProcessingManagement bulkManagement, EntityActionsRegistry registry,
			ActionParameterComponentProvider parameterFactory)
	{
		this.msg = msg;
		this.bulkManagement = bulkManagement;
		this.registry = registry;
		this.parameterFactory = parameterFactory;
		init();
	}

	private void init()
	{
		main = new VerticalLayout();
		main.setMargin(new MarginInfo(true, false, false, false));
		main.addStyleName(Styles.visibleScroll.toString());
		setCompositionRoot(main);
		setCaption(msg.getMessage("BulkProcessingComponent.caption"));

		Button invokeSingle = new Button(
				msg.getMessage("BulkProcessingComponent.performAction"));
		invokeSingle.setDescription(
				msg.getMessage("BulkProcessingComponent.invokeSingleDesc"));
		invokeSingle.addClickListener(event -> showImmediateProcessingDialog(null));
		main.addComponent(invokeSingle);

		viewer = new ScheduledRuleViewerPanel(msg, registry);

		table = new GenericElementsTable<>(
				msg.getMessage("BulkProcessingComponent.tableCaption"),
				this::getCompactName);

		table.setWidth(90, Unit.PERCENTAGE);
		table.setMultiSelect(true);
		table.addSelectionListener(event -> {
			Collection<ScheduledProcessingRule> items = event.getAllSelectedItems();
			if (items.size() > 1 || items.isEmpty())
			{
				viewer.setInput(null);
				return;
			}
			viewer.setInput(items.iterator().next());

		});
		table.addActionHandler(getRefreshAction());
		table.addActionHandler(getAddAction());
		table.addActionHandler(getEditAction());
		table.addActionHandler(getRunScheduledAction());
		table.addActionHandler(getDeleteAction());

		Toolbar<ScheduledProcessingRule> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		table.addSelectionListener(toolbar.getSelectionListener());
		toolbar.addActionHandlers(table.getActionHandlers());
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(table, toolbar);
		tableWithToolbar.setWidth(90, Unit.PERCENTAGE);
		tableWithToolbar.setHeight(100, Unit.PERCENTAGE);

		HorizontalLayout hl = new HorizontalLayout();
		hl.addComponents(tableWithToolbar, viewer);
		hl.setSizeFull();
		hl.setMargin(new MarginInfo(true, false, true, false));

		main.addComponent(hl);
		refresh();
	}

	private String getCompactName(ScheduledProcessingRule rule)
	{
		return rule.getCronExpression() + " - " + rule.getAction().getName();
	}

	private void delete(Set<String> ids)
	{
		for (String id : ids)
		{
			try
			{
				bulkManagement.removeScheduledRule(id);
			} catch (Exception e)
			{
				NotificationPopup.showError(msg, msg.getMessage(
						"BulkProcessingComponent.errorRemoving", id), e);
				refresh();
				return;
			}
		}
		refresh();
	}

	private void refresh()
	{
		try
		{
			List<ScheduledProcessingRule> scheduledRules = bulkManagement
					.getScheduledRules();
			table.setInput(scheduledRules);
			setCompositionRoot(main);
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("BulkProcessingComponent.errorReading"), e);
			setCompositionRoot(error);
		}
	}

	private void schedule(ScheduledProcessingRuleParam rule)
	{
		try
		{
			bulkManagement.scheduleRule(rule);
			refresh();
		} catch (Exception e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("BulkProcessingComponent.errorAdd"), e);
		}
	}

	private void invoke(TranslationRule rule)
	{
		try
		{
			bulkManagement.applyRule(rule);
			NotificationPopup.showSuccess(msg.getMessage("BulkProcessingComponent.actionInvoked"), "");
			refresh();
		} catch (Exception e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("BulkProcessingComponent.errorPerform"), e);
		}
	}

	private SingleActionHandler<ScheduledProcessingRule> getDeleteAction()
	{
		return SingleActionHandler.builder4Delete(msg, ScheduledProcessingRule.class)
				.withHandler(this::deleteHandler).build();
	}

	private void deleteHandler(Collection<ScheduledProcessingRule> items)
	{
		Set<String> removed = items.stream().map(item -> item.getId())
				.collect(Collectors.toSet());
		ConfirmDialog confirm = new ConfirmDialog(msg, msg.getMessage(
				"BulkProcessingComponent.confirmDelete", items.size()), () -> {
					delete(removed);
				});
		confirm.show();
	}

	private SingleActionHandler<ScheduledProcessingRule> getRefreshAction()
	{
		return SingleActionHandler.builder4Refresh(msg, ScheduledProcessingRule.class)
				.withHandler(selection -> refresh()).build();
	}

	private SingleActionHandler<ScheduledProcessingRule> getAddAction()
	{
		return SingleActionHandler.builder4Add(msg, ScheduledProcessingRule.class)
				.withHandler(this::showAddDialog).build();
	}

	private void showAddDialog(Collection<ScheduledProcessingRule> target)
	{
		ActionEditor actionEditor;
		try
		{
			actionEditor = getActionEditor();
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"BulkProcessingComponent.errorCreateActions"), e);
			return;
		}
		ScheduledRuleParamEditorImpl editor = new ScheduledRuleParamEditorImpl(msg,
				actionEditor);
		RuleEditDialog<ScheduledProcessingRuleParam> dialog = new RuleEditDialog<>(msg,
				msg.getMessage("BulkProcessingComponent.addAction"), editor,
				rule -> {
					schedule(rule);
				});
		dialog.show();
	}

	private SingleActionHandler<ScheduledProcessingRule> getEditAction()
	{
		return SingleActionHandler.builder4Edit(msg, ScheduledProcessingRule.class)
				.withHandler(this::showEditDialog).build();
	}

	private void showEditDialog(Collection<ScheduledProcessingRule> target)
	{
		ActionEditor actionEditor;
		try
		{
			actionEditor = getActionEditor();
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"BulkProcessingComponent.errorCreateActions"), e);
			return;
		}
		ScheduledRuleParamEditorImpl editor = new ScheduledRuleParamEditorImpl(msg,
				actionEditor);
		final ScheduledProcessingRule selected = new ScheduledProcessingRule(target.iterator().next());
		editor.setInput(selected);

		RuleEditDialog<ScheduledProcessingRuleParam> dialog = new RuleEditDialog<>(msg,
				msg.getMessage("BulkProcessingComponent.editAction"), editor,
				rule -> 
				{
					delete(Sets.newHashSet(selected.getId()));
					schedule(rule);
				});
		dialog.show();

	}

	private SingleActionHandler<ScheduledProcessingRule> getRunScheduledAction()
	{
		return SingleActionHandler.builder(ScheduledProcessingRule.class)
				.withCaption(msg.getMessage("BulkProcessingComponent.runNowAction"))
				.withIcon(Images.play.getResource())
				.withHandler(this::runScheduledHandler).build();

	}

	private void runScheduledHandler(Collection<ScheduledProcessingRule> items)
	{
		ScheduledProcessingRule rule = new ScheduledProcessingRule(items.iterator().next());
		showImmediateProcessingDialog(rule);
	}

	private void showImmediateProcessingDialog(TranslationRule orig)
	{
		ActionEditor actionEditor;
		try
		{	
			actionEditor = getActionEditor();
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"BulkProcessingComponent.errorCreateActions"), e);
			return;
		}
		RuleEditorImpl editor = new RuleEditorImpl(msg, actionEditor);
		if (orig != null)
			editor.setInput(orig);
		RuleEditDialog<TranslationRule> dialog = new RuleEditDialog<>(msg,
				msg.getMessage("BulkProcessingComponent.performAction"), editor,
				rule -> {
					invoke(rule);
				});
		dialog.show();
	}

	private ActionEditor getActionEditor() throws EngineException
	{
		parameterFactory.init();
		return new ActionEditor(msg, registry, null, parameterFactory);
	}
}
