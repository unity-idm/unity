/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributeclass;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.AttributeClassManagement;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;


/**
 * Responsible for {@link AttributesClass}es management.
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AttributesClassesComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private AttributeClassManagement acMan;
	private AttributeTypeManagement attrMan;
	
	private GenericElementsTable<String> table;
	private AttributesClassViewer viewer;
	private com.vaadin.ui.Component main;
	private Map<String, AttributesClass> allACs;
	
	@Autowired
	public AttributesClassesComponent(UnityMessageSource msg,
			AttributeTypeManagement attributesMan, AttributeClassManagement acMan)
	{
		super();
		this.msg = msg;
		this.attrMan = attributesMan;
		this.acMan = acMan;
		
		init();
	}
	
	private void init()
	{
		setMargin(false);
		addStyleName(Styles.visibleScroll.toString());
		setCaption(msg.getMessage("AttributesClass.caption"));
		viewer = new AttributesClassViewer(msg);
		table = new GenericElementsTable<>(
				msg.getMessage("AttributesClass.attributesClassesHeader"));
		table.setMultiSelect(true);
		table.addSelectionListener(event ->
		{
			Collection<String> items = table.getSelectedItems();
			if (items.size() > 1 || items.isEmpty())
			{
				viewer.setInput(null, allACs);
				return;
			}
			String item = items.iterator().next();	
			if (item != null)
			{
				viewer.setInput(item, allACs);
			} else
				viewer.setInput(null, allACs);
		});
		table.addActionHandler(getRefreshAction());
		table.addActionHandler(getAddAction());
		table.addActionHandler(getEditAction());
		table.addActionHandler(getDeleteAction());
		table.setWidth(90, Unit.PERCENTAGE);
		Toolbar<String> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		table.addSelectionListener(toolbar.getSelectionListener());
		toolbar.addActionHandlers(table.getActionHandlers());
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(table, toolbar);
		tableWithToolbar.setWidth(90, Unit.PERCENTAGE);
		
		
		HorizontalLayout hl = new HorizontalLayout();
		hl.addComponents(tableWithToolbar, viewer);
		hl.setSizeFull();
		hl.setMargin(new MarginInfo(true, false, true, false));
		main = hl;
		refresh();
	}
	
	public void refresh()
	{
		try
		{
			allACs = acMan.getAttributeClasses();
			table.setInput(allACs.keySet());
			viewer.setInput(null, allACs);
			removeAllComponents();
			addComponent(main);
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("AttributesClass.errorGetAttributeClasses"), e);
			removeAllComponents();
			addComponent(error);
		}
		
	}

	
	private boolean addAC(AttributesClass ac)
	{
		try
		{
			acMan.addAttributeClass(ac);
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("AttributesClass.errorAdd"), e);
			return false;
		}
	}

	private boolean updateAC(AttributesClass ac)
	{
		try
		{
			acMan.updateAttributeClass(ac);
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("AttributesClass.errorUpdate"), e);
			return false;
		}
	}
	
	private boolean removeAC(String toRemove)
	{
		try
		{
			acMan.removeAttributeClass(toRemove);
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("AttributesClass.errorRemove"), e);
			return false;
		}
	}
	
	private SingleActionHandler<String> getRefreshAction()
	{
		return SingleActionHandler.builder4Refresh(msg, String.class)
				.withHandler(selection -> refresh())
				.build();
	}
	
	private SingleActionHandler<String> getAddAction()
	{
		return SingleActionHandler.builder4Add(msg, String.class)
				.withHandler(this::showAddDialog)
				.build();
	}
	
	public void showAddDialog(Collection<String> selection)
	{
		Collection<AttributeType> allTypes;
		try
		{
			allTypes = attrMan.getAttributeTypes();
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("AttributesClass.errorGetAttributeTypes"), e);
			return;
		}
		AttributesClassEditor editor = new AttributesClassEditor(msg, allACs, allTypes);
		AttributesClassEditDialog dialog = new AttributesClassEditDialog(msg, 
				msg.getMessage("AttributesClass.addACCaption"), editor, 
				newAC -> addAC(newAC));
		dialog.show();
	}
	
	private SingleActionHandler<String> getEditAction()
	{
		return SingleActionHandler.builder4Edit(msg, String.class)
				.withHandler(this::showEditDialog)
				.build();
	}
	
	private void showEditDialog(Set<String> items)
	{
		String item = items.iterator().next();
		Collection<AttributeType> allTypes;
		try
		{
			allTypes = attrMan.getAttributeTypes();
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage("AttributesClass.errorGetAttributeTypes"), e);
			return;
		}
		AttributesClassEditor editor = new AttributesClassEditor(msg, allACs, allTypes);
		editor.setEditedClass(allACs.get(item));
		AttributesClassEditDialog dialog = new AttributesClassEditDialog(msg, 
				msg.getMessage("AttributesClass.addACCaption"), editor, 
				newAC -> updateAC(newAC));
		dialog.show();
	}
	
	private SingleActionHandler<String> getDeleteAction()
	{
		return SingleActionHandler.builder4Delete(msg, String.class)
				.withHandler(this::handleDelete)
				.build();
	}
	
	private void handleDelete(Set<String> items)
	{
		String confirmText = MessageUtils.createConfirmFromStrings(msg, items);
		new ConfirmDialog(msg, msg.getMessage("AttributesClass.confirmDelete", confirmText),
		() -> {
			for (String item : items)
				removeAC(item);
		}).show();
	}

}
