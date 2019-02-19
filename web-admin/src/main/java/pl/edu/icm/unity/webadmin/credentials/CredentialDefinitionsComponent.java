/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.credentials;

import java.util.Collection;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webadmin.credentials.CredentialDefinitionChangedEvent;
import io.imunity.webadmin.credentials.CredentialDefinitionEditor;
import io.imunity.webadmin.credentials.CredentialDefinitionViewer;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorFactory;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;

/**
 * Provides {@link CredentialDefinition} management UI
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CredentialDefinitionsComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private CredentialManagement credentialMan;
	private CredentialEditorRegistry credentialEditorReg;
	private EventsBus bus;
	
	private GenericElementsTable<CredentialDefinition> table;
	private CredentialDefinitionViewer viewer;
	private com.vaadin.ui.Component main;
	
	@Autowired
	public CredentialDefinitionsComponent(UnityMessageSource msg, CredentialEditorRegistry credentialEditorReg,
			CredentialManagement credentialMan)
	{
		super();
		this.msg = msg;
		this.credentialEditorReg = credentialEditorReg;
		this.credentialMan = credentialMan;
		this.bus = WebSession.getCurrent().getEventBus();
		init();
	}
	
	private void init()
	{
		setMargin(false);
		addStyleName(Styles.visibleScroll.toString());
		setCaption(msg.getMessage("CredentialDefinitions.caption"));
		viewer = new CredentialDefinitionViewer(msg);
		table =  new GenericElementsTable<>(
				msg.getMessage("CredentialDefinitions.credentialDefinitionsHeader"), 
				el -> el.getName());
		table.setStyleGenerator(item -> item.isReadOnly() ? 
				Styles.readOnlyTableElement.toString() : null);
		table.setMultiSelect(true);
		table.addSelectionListener(event ->
		{
			Collection<CredentialDefinition> items = table.getSelectedItems();
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
		});

		table.setWidth(90, Unit.PERCENTAGE);
		table.addActionHandler(getRefreshAction());
		table.addActionHandler(getAddAction());
		table.addActionHandler(getEditAction());
		table.addActionHandler(getDeleteAction());
		Toolbar<CredentialDefinition> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
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
			Collection<CredentialDefinition> crs = credentialMan.getCredentialDefinitions();
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
			credentialMan.updateCredentialDefinition(cd, desiredCredState);
			refresh();
			bus.fireEvent(new CredentialDefinitionChangedEvent(true, cd.getName()));
			if (desiredCredState == LocalCredentialState.outdated)
				NotificationPopup.showNotice(msg.getMessage("notice"), 
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
			credentialMan.addCredentialDefinition(cd);
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
			credentialMan.removeCredentialDefinition(toRemove);
			refresh();
			bus.fireEvent(new CredentialDefinitionChangedEvent(false, toRemove));
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("CredentialDefinitions.errorRemove"), e);
			return false;
		}
	}

	private SingleActionHandler<CredentialDefinition> getRefreshAction()
	{
		return SingleActionHandler.builder4Refresh(msg, CredentialDefinition.class)
				.withHandler(selection -> refresh())
				.build();
	}
	
	private SingleActionHandler<CredentialDefinition> getAddAction()
	{
		return SingleActionHandler.builder4Add(msg, CredentialDefinition.class)
				.withHandler(this::showAddCredDialog)
				.build();
	}
	
	private void showAddCredDialog(Set<CredentialDefinition> dummy)
	{
		CredentialDefinitionEditor editor = new CredentialDefinitionEditor(msg, credentialEditorReg);
		CredentialDefinitionEditDialog dialog = new CredentialDefinitionEditDialog(msg, 
				msg.getMessage("CredentialDefinitions.addAction"), editor, 
				(cd, desiredCredState) -> addCD(cd));
		dialog.show();
	}

	private SingleActionHandler<CredentialDefinition> getEditAction()
	{
		return SingleActionHandler.builder4Edit(msg, CredentialDefinition.class)
				.withHandler(this::showEditCredDialog)
				.withDisabledPredicate(cr -> cr.isReadOnly())
				.build();
	}
	
	private void showEditCredDialog(Set<CredentialDefinition> target)
	{
		CredentialDefinition cr = target.iterator().next();
		CredentialDefinition crClone = cr.clone();
		
		CredentialDefinitionEditor editor = new CredentialDefinitionEditor(msg, credentialEditorReg,
				crClone);
		CredentialDefinitionEditDialog dialog = new CredentialDefinitionEditDialog(msg, 
				msg.getMessage("CredentialDefinitions.editAction"), editor, 
				(cd, desiredCredState) -> updateCD(cd, desiredCredState));
		dialog.show();
	}

	
	private SingleActionHandler<CredentialDefinition> getDeleteAction()
	{
		return SingleActionHandler.builder4Delete(msg, CredentialDefinition.class)
				.withHandler(this::handleDelete)
				.withDisabledPredicate(cr -> cr.isReadOnly())
				.build();
	}
	
	private void handleDelete(Set<CredentialDefinition> items)
	{		
		String confirmText = MessageUtils.createConfirmFromNames(msg, items);

		new ConfirmDialog(msg, msg.getMessage("CredentialDefinitions.confirmDelete", confirmText),
				() ->
				{
					for (CredentialDefinition item : items)
						removeCD(item.getName());
				}).show();
	}
}
