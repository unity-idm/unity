/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.credentials;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.webadmin.credentials.CredentialDefinitionEditDialog.Callback;
import pl.edu.icm.unity.webadmin.utils.MessageUtils;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.GenericElementsTable.GenericItem;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorFactory;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

/**
 * Provides {@link CredentialDefinition} management UI
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CredentialDefinitionsComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private AuthenticationManagement authenticationMan;
	private CredentialEditorRegistry credentialEditorReg;
	private EventsBus bus;
	
	private GenericElementsTable<CredentialDefinition> table;
	private CredentialDefinitionViewer viewer;
	private com.vaadin.ui.Component main;
	
	@Autowired
	public CredentialDefinitionsComponent(UnityMessageSource msg, CredentialEditorRegistry credentialEditorReg,
			AuthenticationManagement authenticationMan)
	{
		super();
		this.msg = msg;
		this.credentialEditorReg = credentialEditorReg;
		this.authenticationMan = authenticationMan;
		this.bus = WebSession.getCurrent().getEventBus();
		init();
	}
	
	private void init()
	{
		setCaption(msg.getMessage("CredentialDefinitions.caption"));
		viewer = new CredentialDefinitionViewer(msg);
		table =  new GenericElementsTable<CredentialDefinition>(
				msg.getMessage("CredentialDefinitions.credentialDefinitionsHeader"), 
				CredentialDefinition.class, new GenericElementsTable.NameProvider<CredentialDefinition>()
				{
					@Override
					public String toRepresentation(CredentialDefinition element)
					{
						return element.getName();
					}
				});
		table.setMultiSelect(true);
		table.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				Collection<CredentialDefinition> items = getItems(table.getValue());
				if (items.size() > 1 || items.isEmpty())
				{
					viewer.setInput(null, null);
					return;
				}	
				CredentialDefinition item = items.iterator().next();
				if (item != null)
				{
					CredentialDefinition cd = item;
					CredentialEditorFactory cef = CredentialDefinitionsComponent.this.
							credentialEditorReg.getFactory(cd.getTypeId());
					viewer.setInput(cd, cef);
				} else
					viewer.setInput(null, null);
			}
		});
		table.setWidth(90, Unit.PERCENTAGE);
		table.addActionHandler(new RefreshActionHandler());
		table.addActionHandler(new AddActionHandler());
		table.addActionHandler(new EditActionHandler());
		table.addActionHandler(new DeleteActionHandler());
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
			Collection<CredentialDefinition> crs = authenticationMan.getCredentialDefinitions();
			table.setInput(crs);
			removeAllComponents();
			addComponent(main);
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("CredentialDefinitions.errorGetCredentialDefinitions"), e);
			removeAllComponents();
			addComponent(error);
		}
		
	}

	private boolean updateCD(CredentialDefinition cd, LocalCredentialState desiredCredState)
	{
		try
		{
			authenticationMan.updateCredentialDefinition(cd, desiredCredState);
			refresh();
			bus.fireEvent(new CredentialDefinitionChangedEvent(true, cd.getName()));
			if (desiredCredState == LocalCredentialState.outdated)
				NotificationPopup.showNotice(msg, msg.getMessage("notice"), 
						msg.getMessage("CredentialDefinitions.outdatedUpdateInfo"));
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("CredentialDefinitions.errorUpdate"), e);
			return false;
		}
	}

	private boolean addCD(CredentialDefinition cd)
	{
		try
		{
			authenticationMan.addCredentialDefinition(cd);
			refresh();
			bus.fireEvent(new CredentialDefinitionChangedEvent(false, cd.getName()));
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("CredentialDefinitions.errorAdd"), e);
			return false;
		}
	}

	private boolean removeCD(String toRemove)
	{
		try
		{
			authenticationMan.removeCredentialDefinition(toRemove);
			refresh();
			bus.fireEvent(new CredentialDefinitionChangedEvent(false, toRemove));
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("CredentialDefinitions.errorRemove"), e);
			return false;
		}
	}

	private Collection<CredentialDefinition> getItems(Object target)
	{
		Collection<?> c = (Collection<?>) target;
		Collection<CredentialDefinition> items = new ArrayList<CredentialDefinition>();
		for (Object o: c)
		{
			GenericItem<?> i = (GenericItem<?>) o;
			items.add((CredentialDefinition) i.getElement());	
		}
		return items;
	}
	
	private class RefreshActionHandler extends SingleActionHandler
	{
		public RefreshActionHandler()
		{
			super(msg.getMessage("CredentialDefinitions.refreshAction"), Images.refresh.getResource());
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
			super(msg.getMessage("CredentialDefinitions.addAction"), Images.add.getResource());
			setNeedsTarget(false);
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			CredentialDefinitionEditor editor = new CredentialDefinitionEditor(msg, credentialEditorReg);
			CredentialDefinitionEditDialog dialog = new CredentialDefinitionEditDialog(msg, 
					msg.getMessage("CredentialDefinitions.addAction"), editor, 
					new Callback()
					{
						@Override
						public boolean newCredentialDefinition(CredentialDefinition cd,
								LocalCredentialState desiredCredState)
						{
							return addCD(cd);
						}
					});
			dialog.show();
		}
	}
	
	private class EditActionHandler extends SingleActionHandler
	{
		public EditActionHandler()
		{
			super(msg.getMessage("CredentialDefinitions.editAction"), Images.edit.getResource());
		}

		@Override
		public void handleAction(Object sender, final Object target)
		{
			
			GenericItem<?> item = (GenericItem<?>) target;
			CredentialDefinition cr = (CredentialDefinition) item.getElement();
			CredentialDefinition crClone = cr.clone();
			
			CredentialDefinitionEditor editor = new CredentialDefinitionEditor(msg, credentialEditorReg,
					crClone);
			CredentialDefinitionEditDialog dialog = new CredentialDefinitionEditDialog(msg, 
					msg.getMessage("CredentialDefinitions.editAction"), editor, 
					new Callback()
					{
						@Override
						public boolean newCredentialDefinition(CredentialDefinition cd,
								LocalCredentialState desiredCredState)
						{
							return updateCD(cd, desiredCredState);
						}
					});
			dialog.show();
		}
	}

	private class DeleteActionHandler extends SingleActionHandler
	{
		public DeleteActionHandler()
		{
			super(msg.getMessage("CredentialDefinitions.deleteAction"), 
					Images.delete.getResource());
			setMultiTarget(true);
		}
		
		@Override
		public void handleAction(Object sender, Object target)
		{		
			final Collection<CredentialDefinition> items = getItems(target);
			String confirmText = MessageUtils.createConfirmFromNames(msg, items);
	
			new ConfirmDialog(msg, msg.getMessage("CredentialDefinitions.confirmDelete", confirmText),
					new ConfirmDialog.Callback()
					{
						@Override
						public void onConfirm()
						{
							for (CredentialDefinition item : items)
							{
								removeCD(item.getName());
							}
						}
					}).show();
		}
	}
}
