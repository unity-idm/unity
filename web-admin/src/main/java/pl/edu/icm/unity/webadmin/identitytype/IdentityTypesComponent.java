/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identitytype;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.webadmin.identitytype.IdentityTypeEditDialog.Callback;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.GenericElementsTable.GenericItem;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;

import com.google.common.collect.Lists;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Responsible for identity types management.
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class IdentityTypesComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	
	private GenericElementsTable<IdentityType> table;
	private IdentityTypeViewer viewer;
	private com.vaadin.ui.Component main;
	private EventsBus bus;

	private IdentitiesManagement identitiesManagement;
	
	
	@Autowired
	public IdentityTypesComponent(UnityMessageSource msg, IdentitiesManagement identitiesManagement)
	{
		this.msg = msg;
		this.identitiesManagement = identitiesManagement;
		this.bus = WebSession.getCurrent().getEventBus();
		HorizontalLayout hl = new HorizontalLayout();
		
		addStyleName(Styles.visibleScroll.toString());
		setCaption(msg.getMessage("IdentityTypes.caption"));
		table = new GenericElementsTable<IdentityType>(msg.getMessage("IdentityTypes.types"), 
				new GenericElementsTable.NameProvider<IdentityType>()
				{
					@Override
					public Label toRepresentation(IdentityType element)
					{
						Label ret = new Label(element.getIdentityTypeProvider().getId());
						if (element.getIdentityTypeProvider().isDynamic())
							ret.addStyleName(Styles.immutableAttribute.toString());
						return ret;
					}
				});

		viewer = new IdentityTypeViewer(msg);
		table.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				Collection<IdentityType> items = getItems(table.getValue());
				if (items.size() > 1 || items.isEmpty())
				{
					viewer.setInput(null);
					return;		
				}	
				IdentityType at = items.iterator().next();	
				if (at != null)
					viewer.setInput(at);
				else
					viewer.setInput(null);
			}
		});
		table.addActionHandler(new RefreshActionHandler());
		table.addActionHandler(new EditActionHandler());
		
		Toolbar toolbar = new Toolbar(table, Orientation.HORIZONTAL);
		toolbar.addActionHandlers(table.getActionHandlers());
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(table, toolbar);
		tableWithToolbar.setWidth(90, Unit.PERCENTAGE);

		hl.addComponents(tableWithToolbar, viewer);
		hl.setSizeFull();
		hl.setMargin(true);
		hl.setSpacing(true);
		hl.setMargin(new MarginInfo(true, false, true, false));
		main = hl;
		refresh();
	}
	
	public void refresh()
	{
		try
		{
			Collection<IdentityType> types = identitiesManagement.getIdentityTypes();
			table.setInput(types);
			removeAllComponents();
			addComponent(main);
			bus.fireEvent(new IdentityTypesUpdatedEvent(types));
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("IdentityTypes.errorGetTypes"), e);
			removeAllComponents();
			addComponent(error);
		}
		
	}
	
	private boolean updateType(IdentityType type)
	{
		try
		{
			identitiesManagement.updateIdentityType(type);
			refresh();
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("IdentityTypes.errorUpdate"), e);
			return false;
		}
	}

	private class RefreshActionHandler extends SingleActionHandler
	{
		public RefreshActionHandler()
		{
			super(msg.getMessage("IdentityTypes.refreshAction"), Images.refresh.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			refresh();
		}
	}

	private Collection<IdentityType> getItems(Object target)
	{
		if (target == null)
			return new ArrayList<>();
		GenericItem<?> i = (GenericItem<?>) target;
		IdentityType at = (IdentityType) i.getElement();
		return Lists.newArrayList(at);
	}
	
	private class EditActionHandler extends SingleActionHandler
	{
		public EditActionHandler()
		{
			super(msg.getMessage("IdentityTypes.editAction"), Images.edit.getResource());
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			
			GenericItem<?> item = (GenericItem<?>) target;	
			IdentityType at = (IdentityType) item.getElement();
			IdentityTypeEditor editor = new IdentityTypeEditor(msg, at);
			IdentityTypeEditDialog dialog = new IdentityTypeEditDialog(msg, 
					msg.getMessage("IdentityTypes.editAction"), new Callback()
					{
						@Override
						public boolean updatedIdentityType(IdentityType newIdentityType)
						{
							return updateType(newIdentityType);
						}
					}, editor);
			dialog.show();
		}
	}
}
