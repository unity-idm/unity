/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.identities;

import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webadmin.attribute.AttributeChangedEvent;
import io.imunity.webadmin.attributetype.AttributeTypesUpdatedEvent;
import io.imunity.webadmin.credentialRequirements.CredentialRequirementChangedEvent;
import io.imunity.webadmin.credentials.CredentialDefinitionChangedEvent;
import io.imunity.webadmin.directoryBrowser.GroupChangedEvent;
import io.imunity.webadmin.idcreate.EntityCreationHandler;
import io.imunity.webadmin.identities.AddAttributeColumnDialog;
import io.imunity.webadmin.identities.AddFilterDialog;
import io.imunity.webadmin.identities.AddToGroupHandler;
import io.imunity.webadmin.identities.DeleteEntityHandler;
import io.imunity.webadmin.identities.EntityFilter;
import io.imunity.webadmin.identities.EntityMergeHandler;
import io.imunity.webadmin.identities.IdentityConfirmHandler;
import io.imunity.webadmin.identities.IdentityConfirmationResendHandler;
import io.imunity.webadmin.identities.IdentityEntry;
import io.imunity.webadmin.identities.RemoveAttributeColumnDialog;
import io.imunity.webadmin.identities.RemoveFromGroupHandler;
import io.imunity.webadmin.reg.invitations.InvitationEntry;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ErrorComponent.Level;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SearchField;
import pl.edu.icm.unity.webui.common.SidebarStyles;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.common.grid.FilterableGridHelper;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Component wrapping {@link IdentitiesTreeGrid}. Allows to configure its mode,
 * feeds it with data to be visualised etc.
 * 
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class IdentitiesPanel extends SafePanel
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, IdentitiesPanel.class);
	private UnityMessageSource msg;
	private AttributeTypeManagement attrsMan;

	private VerticalLayout main;
	private IdentitiesTreeGrid identitiesTable;

	private Toolbar<InvitationEntry> toolbar;
	private HorizontalLayout filtersBar;
	private EntityFilter fastSearchFilter;
	private EventsBus bus;
	private String entityNameAttribute = null;
	private SearchField searchText;
	
	@Autowired
	public IdentitiesPanel(UnityMessageSource msg, AttributeTypeManagement attrsMan,
			RemoveFromGroupHandler removeFromGroupHandler, AddToGroupHandler addToGroupHandler,
			EntityCreationHandler entityCreationDialogHandler, DeleteEntityHandler deleteEntityHandler,
			IdentityConfirmationResendHandler confirmationResendHandler,
			IdentityConfirmHandler confirmHandler, EntityMergeHandler entityMergeHandler,
			IdentitiesTreeGrid identitiesTable, AttributeSupport attributeSupport)
	{
		this.msg = msg;
		this.identitiesTable = identitiesTable;
		this.attrsMan = attrsMan;
			
		try
		{
			AttributeType nameAt = attributeSupport
					.getAttributeTypeWithSingeltonMetadata(EntityNameMetadataProvider.NAME);
			this.entityNameAttribute = nameAt == null ? null : nameAt.getName();
		} catch (EngineException e)
		{
			log.error("Can not determine name attribute");
		}
		
		main = new VerticalLayout();
		main.addStyleName(Styles.visibleScroll.toString());

		toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		toolbar.setWidth(100, Unit.PERCENTAGE);
		toolbar.addHamburger(getHamburgerMenu(removeFromGroupHandler, addToGroupHandler,
				entityCreationDialogHandler, deleteEntityHandler, confirmationResendHandler,
				confirmHandler, entityMergeHandler));
		toolbar.addSearch(getSearchField(), Alignment.MIDDLE_RIGHT);

		filtersBar = new HorizontalLayout();
		filtersBar.addComponent(new Label(msg.getMessage("Identities.filters")));
		filtersBar.setMargin(false);
		filtersBar.setSpacing(false);
		filtersBar.setVisible(false);

		main.addComponents(toolbar, filtersBar, identitiesTable);
		main.setExpandRatio(identitiesTable, 1);
		main.setMargin(new MarginInfo(true, false));
		main.setSpacing(false);
		main.setSizeFull();

		setSizeFull();
		setContent(main);
		setStyleName(Styles.vPanelLight.toString());
		setCaption(msg.getMessage("Identities.caption"));

		bus = WebSession.getCurrent().getEventBus();

		bus.addListener(event -> setGroup(event.getGroup()), GroupChangedEvent.class);

		bus.addListener(event -> setGroup(identitiesTable.getGroup()), CredentialRequirementChangedEvent.class);

		bus.addListener(event -> {
			if (event.isUpdatedExisting())
				setGroup(identitiesTable.getGroup());
		}, CredentialDefinitionChangedEvent.class);

		bus.addListener(event -> setGroup(identitiesTable.getGroup()), AttributeTypesUpdatedEvent.class);

		bus.addListener(event -> {
			Set<String> interestingCurrent = identitiesTable.getAttributeColumns(false);
			interestingCurrent.add(entityNameAttribute);
			String curGroup = identitiesTable.getGroup();
			if (interestingCurrent.contains(event.getAttributeName()) && curGroup.equals(event.getGroup()))
			{
				setGroupWithSelectionSave(curGroup);
				return;
			}
			if (curGroup.equals("/") && curGroup.equals(event.getGroup()))
			{
				Set<String> interestingRoot = identitiesTable.getAttributeColumns(true);
				if (interestingRoot.contains(event.getAttributeName()))
				{
					setGroupWithSelectionSave(curGroup);
					return;
				}
			}		
		}, AttributeChangedEvent.class);

		setGroup(null);
	}
	
	
	
	private void setGroupWithSelectionSave(String group)
	{

		Set<IdentityEntry> selectedItems = identitiesTable.getSelectedItems();
		setGroup(group);

		if (selectedItems.isEmpty())
			return;

		IdentityEntry selectedEntry = selectedItems.iterator().next();
		Identity sourceIdentity = selectedEntry.getSourceIdentity();
		for (IdentityEntry entry : identitiesTable.getItems())
		{
			if (entry.getSourceEntity().getEntity().getId()
					.equals(selectedEntry.getSourceEntity().getEntity().getId())
					&& ((sourceIdentity == null && entry.getSourceIdentity() == null)
							|| (sourceIdentity != null && sourceIdentity
									.equals(entry.getSourceIdentity()))))
			{
				identitiesTable.select(entry);
				identitiesTable.selectionChanged(Sets.newHashSet((entry)));
				identitiesTable.expandParent(entry);
			}
		}

	}

	private SearchField getSearchField()
	{
		searchText = FilterableGridHelper.getRowSearchField(msg);
		searchText.addValueChangeListener(event -> {
			String searched = event.getValue();
			if (fastSearchFilter != null)
				identitiesTable.removeFilter(fastSearchFilter);
			if (searched.isEmpty())
				return;
			fastSearchFilter = e -> e.anyFieldContains(searched, identitiesTable.getVisibleColumnIds());
			identitiesTable.addFilter(fastSearchFilter);
		});

		return searchText;
	}

	private HamburgerMenu<IdentityEntry> getHamburgerMenu(RemoveFromGroupHandler removeFromGroupHandler,
			AddToGroupHandler addToGroupHandler, EntityCreationHandler entityCreationDialogHandler,
			DeleteEntityHandler deleteEntityHandler,
			IdentityConfirmationResendHandler confirmationResendHandler,
			IdentityConfirmHandler confirmHandler, EntityMergeHandler entityMergeHandler)
	{
		HamburgerMenu<IdentityEntry> hamburgerMenu = new HamburgerMenu<>();
		hamburgerMenu.addStyleName(SidebarStyles.sidebar.toString());
		identitiesTable.addSelectionListener(hamburgerMenu.getSelectionListener());

		SingleActionHandler<IdentityEntry> entityCreationAction = entityCreationDialogHandler
				.getAction(identitiesTable::getGroup, added -> refresh());
		hamburgerMenu.addActionHandler(entityCreationAction);
		hamburgerMenu.addSeparator();

		SingleActionHandler<IdentityEntry> addToGroupAction = addToGroupHandler.getAction();
		hamburgerMenu.addActionHandler(addToGroupAction);

		SingleActionHandler<IdentityEntry> removeFromGroupAction = removeFromGroupHandler
				.getAction(identitiesTable::getGroup, this::refresh);
		hamburgerMenu.addActionHandler(removeFromGroupAction);

		SingleActionHandler<IdentityEntry> confirmationResendAction = confirmationResendHandler.getAction();
		hamburgerMenu.addActionHandler(confirmationResendAction);

		SingleActionHandler<IdentityEntry> confirmAction = confirmHandler.getAction(this::refresh);
		hamburgerMenu.addActionHandler(confirmAction);

		SingleActionHandler<IdentityEntry> entityMergeAction = entityMergeHandler
				.getAction(identitiesTable::getGroup);
		hamburgerMenu.addActionHandler(entityMergeAction);

		SingleActionHandler<IdentityEntry> deleteEntityAction = deleteEntityHandler
				.getAction(identitiesTable::removeEntity);
		hamburgerMenu.addActionHandler(deleteEntityAction);

		hamburgerMenu.addSeparator();

		hamburgerMenu.addItem(msg.getMessage("Identities.addFilter"), Images.addFilter.getResource(), c -> {
			List<String> columnIds = identitiesTable.getColumnIds();
			new AddFilterDialog(msg, columnIds, (filter, description) -> addFilterInfo(filter, description))
					.show();
		});

		hamburgerMenu.addItem(msg.getMessage("Identities.addAttributes"), Images.addColumn.getResource(), c -> {
			new AddAttributeColumnDialog(msg, attrsMan, (attributeType, group) -> identitiesTable
					.addAttributeColumn(attributeType, group)).show();
		});

		hamburgerMenu.addItem(msg.getMessage("Identities.removeAttributes"), Images.removeColumn.getResource(),
				c -> {
					Set<String> alreadyUsedRoot = identitiesTable.getAttributeColumns(true);
					Set<String> alreadyUsedCurrent = identitiesTable.getAttributeColumns(false);
					new RemoveAttributeColumnDialog(msg, alreadyUsedRoot, alreadyUsedCurrent,
							identitiesTable.getGroup(),
							(attributeType, group) -> identitiesTable
									.removeAttributeColumn(group, attributeType))
											.show();
				});

		MenuItem showTargeted = hamburgerMenu.addItem(msg.getMessage("Identities.showTargeted"), null, c -> {
			try
			{
				identitiesTable.setShowTargeted(c.isChecked());
			} catch (EngineException e)
			{
				setIdProblem(IdentitiesPanel.this.identitiesTable.getGroup(), e);
			}
		});
		showTargeted.setCheckable(true);
		showTargeted.setChecked(identitiesTable.isShowTargeted());

		MenuItem mode = hamburgerMenu.addItem(msg.getMessage("Identities.mode"), null, c -> {
			identitiesTable.setMode(c.isChecked());
		});

		mode.setCheckable(true);
		mode.setChecked(identitiesTable.isGroupByEntity());
		return hamburgerMenu;
	}

	private void setGroup(String group)
	{
		identitiesTable.clearFilters();
		searchText.clear();
		
		if (group == null)
		{
			try
			{
				identitiesTable.showGroup(null);
			} catch (EngineException e)
			{
				// ignored, shouldn't happen anyway
			}
			setProblem(msg.getMessage("Identities.noGroupSelected"), Level.warning);
			return;
		}
		try
		{
			identitiesTable.showGroup(group);
			identitiesTable.setVisible(true);
			setCaption(msg.getMessage("Identities.caption", group));
			setContent(main);
		} catch (AuthorizationException e)
		{
			setProblem(msg.getMessage("Identities.noReadAuthz", group), Level.error);
		} catch (Exception e)
		{
			setIdProblem(group, e);
		}
	}

	private void setIdProblem(String group, Exception e)
	{
		log.error("Problem retrieving group contents of " + group, e);
		setProblem(msg.getMessage("Identities.internalError", e.toString()), Level.error);
	}

	private void setProblem(String message, Level level)
	{
		ErrorComponent errorC = new ErrorComponent();
		errorC.setMessage(message, level);
		setCaption(msg.getMessage("Identities.captionNoGroup"));
		setContent(errorC);
	}

	private void refresh()
	{
		bus.fireEvent(new GroupChangedEvent(identitiesTable.getGroup()));
	}

	private void addFilterInfo(EntityFilter filter, String description)
	{
		identitiesTable.addFilter(filter);
		filtersBar.addComponent(new FilterInfo(description, filter));
		filtersBar.setVisible(true);
	}

	private class FilterInfo extends HorizontalLayout
	{
		public FilterInfo(String description, EntityFilter filter)
		{
			Label info = new Label(description);
			Button remove = new Button();
			remove.addStyleName(Styles.vButtonLink.toString());
			remove.setIcon(Images.delete.getResource());
			remove.addClickListener(event -> {
				identitiesTable.removeFilter(filter);
				filtersBar.removeComponent(FilterInfo.this);
				if (filtersBar.getComponentCount() == 1)
					filtersBar.setVisible(false);
			});
			addComponents(info, HtmlTag.hspaceEm(1), remove);
			setMargin(new MarginInfo(false, false, false, true));
		}
	}
}
