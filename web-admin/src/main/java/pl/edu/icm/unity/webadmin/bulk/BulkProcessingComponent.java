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

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.BulkProcessingManagement;
import pl.edu.icm.unity.server.bulkops.EntityAction;
import pl.edu.icm.unity.server.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.server.bulkops.ScheduledProcessingRuleParam;
import pl.edu.icm.unity.server.registries.EntityActionsRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.tprofile.ActionEditor;
import pl.edu.icm.unity.webadmin.tprofile.ActionParameterComponentFactory;
import pl.edu.icm.unity.webadmin.tprofile.ActionParameterComponentFactory.Provider;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.GenericElementsTable.GenericItem;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

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
	private ActionParameterComponentFactory parameterFactory;

	private GenericElementsTable<ScheduledProcessingRule> table;
	private ScheduledRuleViewerPanel viewer;
	private VerticalLayout main;
	
	@Autowired
	public BulkProcessingComponent(UnityMessageSource msg,
			BulkProcessingManagement bulkManagement, EntityActionsRegistry registry,
			ActionParameterComponentFactory parameterFactory)
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
		main.addStyleName(Styles.visibleScroll.toString());
		setCompositionRoot(main);
		setCaption(msg.getMessage("BulkProcessingComponent.caption"));
		
		viewer = new ScheduledRuleViewerPanel(msg, registry);
		table = new GenericElementsTable<>(msg.getMessage("BulkProcessingComponent.tableCaption"), 
				ScheduledProcessingRule.class, this::getCompactName);
		table.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				Collection<ScheduledProcessingRule> items = getItems(table.getValue());
				if (items.size() > 1 || items.isEmpty())
				{
					viewer.setInput(null);
					return;
				}
				viewer.setInput(items.iterator().next());
			}
		});
		table.addActionHandler(new RefreshActionHandler());
		table.addActionHandler(new AddActionHandler());
		table.addActionHandler(new DeleteActionHandler());
		table.setWidth(90, Unit.PERCENTAGE);
		table.setMultiSelect(true);
		Toolbar toolbar = new Toolbar(table, Orientation.HORIZONTAL);
		toolbar.addActionHandlers(table.getActionHandlers());
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(table, toolbar);
		tableWithToolbar.setWidth(90, Unit.PERCENTAGE);
		
		HorizontalLayout hl = new HorizontalLayout();
		hl.addComponents(tableWithToolbar, viewer);
		hl.setSizeFull();
		hl.setMargin(new MarginInfo(true, false, true, false));
		hl.setSpacing(true);
		
		main.addComponent(hl);
		refresh();
	}
	
	private String getCompactName(ScheduledProcessingRule rule)
	{
		return rule.getCronExpression() + " - " +
				rule.getAction().getActionDescription().getName();
	}
	
	private Collection<ScheduledProcessingRule> getItems(Object target)
	{
		Collection<?> c = (Collection<?>) target;
		return c.stream()
			.map(o -> (ScheduledProcessingRule)(((GenericItem<?>)o).getElement()))
			.collect(Collectors.toList());
	}
	
	private void delete(Set<String> ids)
	{
		for (String id: ids)
		{
			try
			{
				bulkManagement.removeScheduledRule(id);
			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, 
						msg.getMessage("BulkProcessingComponent.errorRemoving", id), e);
				return;
			}
		}
	}
	
	private void refresh()
	{
		try
		{
			List<ScheduledProcessingRule> scheduledRules = bulkManagement.getScheduledRules();
			table.setInput(scheduledRules);
			setCompositionRoot(main);
		} catch (EngineException e)
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
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, 
					msg.getMessage("BulkProcessingComponent.errorAdd"), e);
		}
	}
	
	private class DeleteActionHandler extends SingleActionHandler
	{
		public DeleteActionHandler()
		{
			super(msg.getMessage("BulkProcessingComponent.deleteAction"), 
					Images.delete.getResource());
			setMultiTarget(true);
		}
		
		@Override
		public void handleAction(Object sender, Object target)
		{		
			final Collection<ScheduledProcessingRule> items = getItems(target);			
			Set<String> removed = items.stream()
					.map(item -> item.getId())
					.collect(Collectors.toSet()); 
			ConfirmDialog confirm = new ConfirmDialog(msg, 
					msg.getMessage("BulkProcessingComponent.confirmDelete", items.size()), 
					() -> {
				delete(removed);
			});
			confirm.show();
		}
	}
	
	private class RefreshActionHandler extends SingleActionHandler
	{
		public RefreshActionHandler()
		{
			super(msg.getMessage("BulkProcessingComponent.refreshAction"), Images.refresh.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			refresh();
		}
	}
	
	private class AddActionHandler extends SingleActionHandler
	{
		public AddActionHandler()
		{
			super(msg.getMessage("BulkProcessingComponent.addAction"), Images.add.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			Provider componentProvider;
			try
			{
				componentProvider = parameterFactory.getComponentProvider();
			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, 
						msg.getMessage("BulkProcessingComponent.errorCreateActions"), e);
				return;
			}
			ActionEditor<EntityAction> actionEditor = new ActionEditor<>(msg, registry, null, 
					componentProvider);
			ScheduledRuleParamEditorImpl editor = new ScheduledRuleParamEditorImpl(msg, actionEditor);
			RuleEditDialog<ScheduledProcessingRuleParam> dialog = new RuleEditDialog<>(msg, 
					msg.getMessage("BulkProcessingComponent.addAction"), editor, 
					rule -> 
					{
						schedule(rule);
					});
			dialog.show();
		}
	}
}
