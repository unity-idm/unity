/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.credreq;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar2;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.GenericElementsTable2;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler2;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar2;

/**
 * Provides {@link CredentialRequirements} management UI
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CredentialRequirementsComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private CredentialRequirementManagement credReqMan;
	private CredentialManagement credMan;
	private EventsBus bus;
	
	private GenericElementsTable2<CredentialRequirements> table;
	private CredentialRequirementViewer viewer;
	private com.vaadin.ui.Component main;
	
	@Autowired
	public CredentialRequirementsComponent(UnityMessageSource msg,
			CredentialRequirementManagement authenticationMan,
			CredentialManagement credMan)
	{
		this.msg = msg;
		this.credReqMan = authenticationMan;
		this.credMan = credMan;
		this.bus = WebSession.getCurrent().getEventBus();
		
		init();
	}
	
	private void init()
	{
		setMargin(false);
		addStyleName(Styles.visibleScroll.toString());
		setCaption(msg.getMessage("CredentialRequirements.caption"));
		viewer = new CredentialRequirementViewer(msg);
		table =  new GenericElementsTable2<>(
				msg.getMessage("CredentialRequirements.credentialRequirementsHeader"),
				cr -> cr.getName());
		table.setStyleGenerator(item -> item.isReadOnly() ? 
				Styles.readOnlyTableElement.toString() : null);
		table.setMultiSelect(true);
		table.addSelectionListener(event ->
		{
			Collection<CredentialRequirements> items = event.getAllSelectedItems();
			if (items.size() > 1 || items.isEmpty())
			{
				viewer.setInput(null);
				return;
			}	
			CredentialRequirements item = items.iterator().next();	
			viewer.setInput(item);
		});
		table.addActionHandler(getRefreshAction());
		table.addActionHandler(getAddAction());
		table.addActionHandler(getEditAction());
		table.addActionHandler(getDeleteAction());
		table.setWidth(90, Unit.PERCENTAGE);
		Toolbar2<CredentialRequirements> toolbar = new Toolbar2<>(Orientation.HORIZONTAL);
		table.addSelectionListener(toolbar.getSelectionListener());
		toolbar.addActionHandlers(table.getActionHandlers());
		ComponentWithToolbar2 tableWithToolbar = new ComponentWithToolbar2(table, toolbar);
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
			Collection<CredentialRequirements> crs = credReqMan.getCredentialRequirements();
			table.setInput(crs);
			removeAllComponents();
			addComponent(main);
		} catch (Exception e)
		{
			ErrorComponent error = new ErrorComponent();
			error.setError(msg.getMessage("CredentialRequirements.errorGetCredentialRequirements"), e);
			removeAllComponents();
			addComponent(error);
		}
		
	}

	public Collection<CredentialRequirements> getCredentialRequirements()
	{
		try
		{
			return credReqMan.getCredentialRequirements();
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("CredentialRequirements.errorGetCredentialRequirements"), e);
			return null;
		}
	}

	private Collection<CredentialDefinition> getCredentials()
	{
		try
		{
			return credMan.getCredentialDefinitions();
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("CredentialRequirements.errorGetCredentials"), e);
			return null;
		}
		
	}

	private boolean updateCR(CredentialRequirements cr)
	{
		try
		{
			credReqMan.updateCredentialRequirement(cr);
			refresh();
			bus.fireEvent(new CredentialRequirementChangedEvent());
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("CredentialRequirements.errorUpdate"), e);
			return false;
		}
	}

	private boolean addCR(CredentialRequirements cr)
	{
		try
		{
			credReqMan.addCredentialRequirement(cr);
			refresh();
			bus.fireEvent(new CredentialRequirementChangedEvent());
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("CredentialRequirements.errorAdd"), e);
			return false;
		}
	}

	private boolean removeCR(String toRemove, String replacementId)
	{
		try
		{
			credReqMan.removeCredentialRequirement(toRemove, replacementId);
			refresh();
			bus.fireEvent(new CredentialRequirementChangedEvent());
			return true;
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("CredentialRequirements.errorRemove"), e);
			return false;
		}
	}
	
	private SingleActionHandler2<CredentialRequirements> getRefreshAction()
	{
		return SingleActionHandler2.builder(
				msg.getMessage("CredentialRequirements.refreshAction"), 
				Images.refresh.getResource(),
				CredentialRequirements.class,
				selection -> refresh())
				.dontRequireTarget()
				.build();
	}

	private SingleActionHandler2<CredentialRequirements> getAddAction()
	{
		return SingleActionHandler2.builder(
					msg.getMessage("CredentialRequirements.addAction"), 
					Images.add.getResource(),
					this::showAddCRDialog)
				.dontRequireTarget()
				.build();
	}
	
	private void showAddCRDialog(Set<CredentialRequirements> target)
	{
		Collection<CredentialDefinition> allCredentials = getCredentials();
		if (allCredentials == null)
			return;
		CredentialRequirementEditor editor = new CredentialRequirementEditor(msg, allCredentials);
		CredentialRequirementEditDialog dialog = new CredentialRequirementEditDialog(msg, 
				msg.getMessage("CredentialRequirements.addAction"), editor, 
				this::addCR);
		dialog.show();
	}
	
	private SingleActionHandler2<CredentialRequirements> getEditAction()
	{
		return SingleActionHandler2.builder(
					msg.getMessage("CredentialRequirements.editAction"), 
					Images.edit.getResource(),
					this::showEditCRDialog)
				.withDisabledPredicate(cr -> cr.isReadOnly())
				.build();
	}
	
	private void showEditCRDialog(Set<CredentialRequirements> target)
	{
		Collection<CredentialDefinition> allCredentials = getCredentials();	
		if (allCredentials == null)
			return;
		CredentialRequirements cr = target.iterator().next();
		CredentialRequirements crClone = new CredentialRequirements();
		crClone.setDescription(cr.getDescription());
		crClone.setName(cr.getName());
		crClone.setRequiredCredentials(new HashSet<>(cr.getRequiredCredentials()));
		CredentialRequirementEditor editor = new CredentialRequirementEditor(msg, allCredentials, crClone);
		CredentialRequirementEditDialog dialog = new CredentialRequirementEditDialog(msg, 
				msg.getMessage("CredentialRequirements.editAction"), editor, 
				this::updateCR);
		dialog.show();
	}
	
	private SingleActionHandler2<CredentialRequirements> getDeleteAction()
	{
		return SingleActionHandler2.builder(
					msg.getMessage("CredentialRequirements.deleteAction"), 
					Images.delete.getResource(),
					this::deleteHandler)
				.withDisabledPredicate(cr -> cr.isReadOnly())
				.multiTarget()
				.build();
	}
	
	private void deleteHandler(Set<CredentialRequirements> items)
	{
		HashSet<String> removed = new HashSet<>();
		for (CredentialRequirements item : items)
		{
			removed.add(item.getName());
			
		}			
		Collection<CredentialRequirements> allCRs = getCredentialRequirements();
		new CredentialRequirementRemovalDialog(msg, removed, allCRs, 
				replacementCR ->
				{
					for (CredentialRequirements item : items)
						removeCR(item.getName(), replacementCR);
				}
		).show();
	}
}
