/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributeclass;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.GenericElementsTable.GenericItem;


/**
 * Responsible for {@link AttributesClass}es management.
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AttributesClassesComponent  extends VerticalLayout
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
		setCaption(msg.getMessage("AttributesClass.caption"));
		table =  new GenericElementsTable<String>(
				msg.getMessage("AttributesClass.attributesClassesHeader"), 
				String.class);
		table.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				@SuppressWarnings("unchecked")
				GenericItem<String> item = (GenericItem<String>)table.getValue();
				if (item != null)
				{
					String ac = item.getElement();
					viewer.setInput(ac, allACs);
				} else
					viewer.setInput(null, allACs);
			}
		});
		table.addActionHandler(new RefreshActionHandler());
		table.addActionHandler(new AddActionHandler());
		table.addActionHandler(new DeleteActionHandler());
		table.setWidth(90, Unit.PERCENTAGE);
		
		viewer = new AttributesClassViewer(msg);
		HorizontalLayout hl = new HorizontalLayout();
		hl.addComponents(table, viewer);
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
			ErrorPopup.showError(msg.getMessage("AttributesClass.errorAdd"), e);
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
			ErrorPopup.showError(msg.getMessage("AttributesClass.errorRemove"), e);
			return false;
		}
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
				ErrorPopup.showError(msg.getMessage("AttributesClass.errorGetAttributeTypes"), e);
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
		}
		
		@Override
		public void handleAction(Object sender, Object target)
		{
			@SuppressWarnings("unchecked")
			final GenericItem<String> item = (GenericItem<String>)target;
			new ConfirmDialog(msg, msg.getMessage("AttributesClass.confirmDelete", item.getElement()),
					new ConfirmDialog.Callback()
			{
				@Override
				public void onConfirm()
				{
					removeAC(item.getElement());
				}
			}).show();
		}
	}

}
