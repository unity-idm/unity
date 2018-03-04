/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identitytype;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.IdentityTypesManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.webadmin.identitytype.IdentityTypeEditDialog.Callback;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;

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

	private IdentityTypesManagement identitiesManagement;

	private IdentityTypeSupport idTypeSupport;
	private MessageTemplateManagement msgTemplateMan;

	@Autowired
	public IdentityTypesComponent(UnityMessageSource msg,
			IdentityTypesManagement identitiesManagement,
			IdentityTypeSupport idTypeSupport,
			MessageTemplateManagement msgTemplateMan)
	{
		this.msg = msg;
		this.identitiesManagement = identitiesManagement;
		this.idTypeSupport = idTypeSupport;
		this.msgTemplateMan = msgTemplateMan;
		this.bus = WebSession.getCurrent().getEventBus();

		setMargin(false);
		HorizontalLayout hl = new HorizontalLayout();
		addStyleName(Styles.visibleScroll.toString());
		setCaption(msg.getMessage("IdentityTypes.caption"));

		table = new GenericElementsTable<IdentityType>(
				msg.getMessage("IdentityTypes.types"),
				element -> element.getIdentityTypeProvider());
		table.setStyleGenerator(
				id -> idTypeSupport.getTypeDefinition(id.getName()).isDynamic()
						? Styles.immutableAttribute.toString()
						: "");

		table.setMultiSelect(true);
		table.setWidth(90, Unit.PERCENTAGE);

		viewer = new IdentityTypeViewer(msg, idTypeSupport);
		table.addSelectionListener(event -> {
			Collection<IdentityType> items = event.getAllSelectedItems();
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

		});

		table.addActionHandler(getRefreshAction());
		table.addActionHandler(getEditAction());
		Toolbar<IdentityType> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		table.addSelectionListener(toolbar.getSelectionListener());
		toolbar.addActionHandlers(table.getActionHandlers());
		ComponentWithToolbar tableWithToolbar = new ComponentWithToolbar(table, toolbar);
		tableWithToolbar.setWidth(90, Unit.PERCENTAGE);
		tableWithToolbar.setHeight(100, Unit.PERCENTAGE);

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
			NotificationPopup.showError(msg,
					msg.getMessage("IdentityTypes.errorUpdate"), e);
			return false;
		}
	}

	private SingleActionHandler<IdentityType> getRefreshAction()
	{
		return SingleActionHandler.builder4Refresh(msg, IdentityType.class)
				.withHandler(selection -> refresh()).build();
	}

	private SingleActionHandler<IdentityType> getEditAction()
	{
		return SingleActionHandler.builder4Edit(msg, IdentityType.class)
				.withHandler(this::showEditDialog).build();
	}

	private void showEditDialog(Collection<IdentityType> target)
	{
		IdentityType idType = target.iterator().next();
		idType = idType.clone();
		IdentityTypeEditor editor = new IdentityTypeEditor(msg, idTypeSupport, msgTemplateMan, idType);
		IdentityTypeEditDialog dialog = new IdentityTypeEditDialog(msg,
				msg.getMessage("IdentityTypes.editAction"), new Callback()
				{
					@Override
					public boolean updatedIdentityType(
							IdentityType newIdentityType)
					{
						return updateType(newIdentityType);
					}
				}, editor);
		dialog.show();
	}
}
