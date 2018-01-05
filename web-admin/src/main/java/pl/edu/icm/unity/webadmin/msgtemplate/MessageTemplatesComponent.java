/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.msgtemplate;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.msgtemplate.MessageTemplateConsumersRegistry;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.webadmin.utils.MessageUtils;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar2;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GenericElementsTable2;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler2;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar2;

/**
 * Responsible for message templates management
 * @author P. Piernik
 *
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MessageTemplatesComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private MessageTemplateManagement msgTempMan;
	private GenericElementsTable2<MessageTemplate> table;
	private MessageTemplateViewer viewer;
	private com.vaadin.ui.Component main;
	private MessageTemplateConsumersRegistry consumersRegistry;
	
	@Autowired
	public MessageTemplatesComponent(UnityMessageSource msg,
			MessageTemplateManagement msgTempMan,
			MessageTemplateConsumersRegistry consumersRegistry)
	{
		this.msg = msg;
		this.msgTempMan = msgTempMan;
		this.consumersRegistry = consumersRegistry;

		setMargin(false);
		addStyleName(Styles.visibleScroll.toString());
		HorizontalLayout hl = new HorizontalLayout();
		setCaption(msg.getMessage("MessageTemplatesComponent.capion"));
		
		table = new GenericElementsTable2<>(msg.getMessage("MessageTemplatesComponent.templatesTable"), 
				element -> element.getName());
		
		table.setMultiSelect(true);
		table.setWidth(90, Unit.PERCENTAGE);
		viewer = new MessageTemplateViewer(msg, consumersRegistry);
		viewer.setTemplateInput(null);
		table.addSelectionListener(event ->
		{
				Collection<MessageTemplate> items = event.getAllSelectedItems();
				if (items.size() > 1 || items.isEmpty())
				{
					viewer.setTemplateInput(null);
					return;	
				}	
				MessageTemplate item = items.iterator().next();
				MessageTemplate forPreview = getTemplateForPreview(item);
				viewer.setTemplateInput(forPreview);
			
		});
		table.addActionHandler(getRefreshAction());
		table.addActionHandler(getAddAction());
		table.addActionHandler(getEditAction());
		table.addActionHandler(getDeleteAction());
		
		Toolbar2<MessageTemplate> toolbar = new Toolbar2<>(Orientation.HORIZONTAL);
		table.addSelectionListener(toolbar.getSelectionListener());
		toolbar.addActionHandlers(table.getActionHandlers());
		ComponentWithToolbar2 tableWithToolbar = new ComponentWithToolbar2(table, toolbar);
		tableWithToolbar.setWidth(90, Unit.PERCENTAGE);
		tableWithToolbar.setHeight(100, Unit.PERCENTAGE);
		
		hl.addComponents(tableWithToolbar, viewer);
		hl.setSizeFull();
		hl.setMargin(new MarginInfo(true, false, true, false));
		main = hl;
		hl.setExpandRatio(tableWithToolbar, 0.3f);
		hl.setExpandRatio(viewer, 0.7f);
		refresh();
	}
	
	private void refresh()
	{
		try
		{
			Collection<MessageTemplate> templates = msgTempMan.listTemplates().values();
			table.setInput(templates);
			viewer.setTemplateInput(null);
			removeAllComponents();
			addComponent(main);
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("MessageTemplatesComponent.errorGetTemplates"), e);
			removeAllComponents();
			addComponent(error);
		}
		
	}
	
	private boolean updateTemplate(MessageTemplate updatedTemplate)
	{
		try
		{
			msgTempMan.updateTemplate(updatedTemplate);
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("MessageTemplatesComponent.errorUpdate"), e);
			return false;
		}
	}

	private MessageTemplate getTemplateForPreview(MessageTemplate srcTemplate)
	{
		try
		{
			return msgTempMan.getPreprocessedTemplate(srcTemplate.getName());
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("MessageTemplatesComponent.errorGetTemplates"), e);
			return null;
		}
	}
	
	private boolean addTemplate(MessageTemplate template)
	{
		try
		{
			msgTempMan.addTemplate(template);
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("MessageTemplatesComponent.errorAdd"), e);
			return false;
		}
	}
	
	private boolean removeTemplate(MessageTemplate template)
	{
		try
		{
			msgTempMan.removeTemplate(template.getName());
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("MessageTemplatesComponent.errorRemove"), e);
			return false;
		}
	}
	
	private SingleActionHandler2<MessageTemplate> getRefreshAction()
	{
		return SingleActionHandler2.builder4Refresh(msg, MessageTemplate.class)
				.withHandler(selection -> refresh())
				.build();
	}
	
	private SingleActionHandler2<MessageTemplate> getAddAction()
	{
		return SingleActionHandler2.builder4Add(msg, MessageTemplate.class)
				.withHandler(this::showAddDialog)
				.build();
	}
	
	
	private void showAddDialog(Collection<MessageTemplate> target)
	{		
		MessageTemplateEditor editor = new MessageTemplateEditor(msg, consumersRegistry, null, msgTempMan);	
		MessageTemplateEditDialog dialog = new MessageTemplateEditDialog(msg, 
				msg.getMessage("MessageTemplatesComponent.addAction"), newTemplate -> addTemplate(newTemplate)
				, editor);
		dialog.show();
	}
	
	private SingleActionHandler2<MessageTemplate> getEditAction()
	{
		return SingleActionHandler2.builder4Edit(msg, MessageTemplate.class)
				.withHandler(this::showEditDialog)
				.build();
	}
	
	private void showEditDialog(Collection<MessageTemplate> target)
	{
		MessageTemplate msgTemp = target.iterator().next();
		msgTemp = msgTemp.clone();
		MessageTemplateEditor editor;
		
		editor = new MessageTemplateEditor(msg, consumersRegistry, msgTemp, msgTempMan);
		
		MessageTemplateEditDialog dialog = new MessageTemplateEditDialog(msg, 
				msg.getMessage("MessageTemplatesComponent.editAction"), newTemplate -> updateTemplate(newTemplate)
				, editor);
		dialog.show();
		
	}
	
	private SingleActionHandler2<MessageTemplate> getDeleteAction()
	{
		return SingleActionHandler2.builder4Delete(msg, MessageTemplate.class)
				.withHandler(this::deleteHandler)
				.build();
	}
	
	private void deleteHandler(Collection<MessageTemplate> items)
	{	
		String confirmText = MessageUtils.createConfirmFromNames(msg, items);
		new ConfirmDialog(msg, msg.getMessage(
				"MessageTemplatesComponent.confirmDelete", confirmText),
				() -> items.forEach(this::removeTemplate)).show();
	}
	
}
