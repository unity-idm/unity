/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributeclass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.webadmin.utils.MessageUtils;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.GenericElementsTable.GenericItem;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Toolbar;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;


/**
 * Responsible for {@link AttributesClass}es management.
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AttributesClassesComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private AttributesManagement attrMan;

	private GenericElementsTable<String> table;
	private AttributesClassViewer viewer;
	private com.vaadin.ui.Component main;
	private Map<String, AttributesClass> allACs;
	
	@Autowired
	public AttributesClassesComponent(UnityMessageSource msg,
			AttributesManagement attributesMan)
	{
		super();
		this.msg = msg;
		this.attrMan = attributesMan;
		
		init();
	}
	
	private void init()
	{
		addStyleName(Styles.visibleScroll.toString());
		setCaption(msg.getMessage("AttributesClass.caption"));
		viewer = new AttributesClassViewer(msg);
		table =  new GenericElementsTable<String>(
				msg.getMessage("AttributesClass.attributesClassesHeader"), 
				String.class);
		table.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				Collection<String> items = getItems(table.getValue());
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
		main = hl;
		refresh();
	}
	
	public void refresh()
	{
		try
		{
			allACs = attrMan.getAttributeClasses();
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
			attrMan.addAttributeClass(ac);
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("AttributesClass.errorAdd"), e);
			return false;
		}
	}

	private boolean removeAC(String toRemove)
	{
		try
		{
			attrMan.removeAttributeClass(toRemove);
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("AttributesClass.errorRemove"), e);
			return false;
		}
	}
	
	private Collection<String> getItems(Object target)
	{
		Collection<?> c = (Collection<?>) target;
		Collection<String> items = new ArrayList<String>();
		for (Object o: c)
		{
			GenericItem<?> i = (GenericItem<?>) o;
			items.add((String) i.getElement());	
		}
		return items;
	}

	private class RefreshActionHandler extends SingleActionHandler
	{
		public RefreshActionHandler()
		{
			super(msg.getMessage("AttributesClass.refreshAction"), Images.refresh.getResource());
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
			super(msg.getMessage("AttributesClass.addAction"), Images.add.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
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
					new AttributesClassEditDialog.Callback()
					{
						@Override
						public boolean newAttributesClass(AttributesClass newAC)
						{
							return addAC(newAC);
						}
					});
			dialog.show();
		}
	}
	
	private class DeleteActionHandler extends SingleActionHandler
	{
		public DeleteActionHandler()
		{
			super(msg.getMessage("AttributesClass.deleteAction"), 
					Images.delete.getResource());
			setMultiTarget(true);
		}
		
		@Override
		public void handleAction(Object sender, Object target)
		{
			final Collection<String> items = getItems(target);
			
			String confirmText = MessageUtils.createConfirmFromStrings(msg, items);
			new ConfirmDialog(msg, msg.getMessage("AttributesClass.confirmDelete", confirmText),
					new ConfirmDialog.Callback()
			{
				@Override
				public void onConfirm()
				{
					for (String item : items)
					{
						removeAC(item);
					}
				}
			}).show();
		}
	}

}
