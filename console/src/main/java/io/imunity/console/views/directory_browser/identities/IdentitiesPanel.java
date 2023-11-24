/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import com.google.common.collect.Sets;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.console.attribute.AttributeChangedEvent;
import io.imunity.console.views.authentication.credential_requirements.CredentialRequirementChangedEvent;
import io.imunity.console.views.directory_browser.AttributeTypesUpdatedEvent;
import io.imunity.console.views.directory_browser.RefreshAndSelectEvent;
import io.imunity.console.views.directory_browser.group_browser.GroupChangedEvent;
import io.imunity.console.views.directory_browser.identities.credentials.CredentialDefinitionChangedEvent;
import io.imunity.vaadin.elements.ErrorLabel;
import io.imunity.vaadin.elements.MenuButton;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.SearchField;
import io.imunity.vaadin.endpoint.common.ActionMenuWithHandlerSupport;
import io.imunity.vaadin.endpoint.common.Toolbar;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.SingleActionHandler;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;

import java.util.List;
import java.util.Set;

import static io.imunity.vaadin.elements.CssClassNames.POINTER;
import static io.imunity.vaadin.elements.CssClassNames.SMALL_GAP;

@PrototypeComponent
public class IdentitiesPanel extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, IdentitiesPanel.class);
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private final AttributeTypeManagement attrsMan;

	private final VerticalLayout main;
	private final IdentitiesTreeGrid identitiesTable;
	private final Toolbar<InvitationEntry> toolbar;
	private final HorizontalLayout filtersBar;
	private final EventsBus bus;

	private EntityFilter fastSearchFilter;
	private String entityNameAttribute = null;
	private SearchField searchText;
	
	IdentitiesPanel(MessageSource msg, AttributeTypeManagement attrsMan,
			RemoveFromGroupHandler removeFromGroupHandler, AddToGroupHandler addToGroupHandler,
			EntityCreationHandler entityCreationDialogHandler, DeleteEntityHandler deleteEntityHandler,
			IdentityConfirmationResendHandler confirmationResendHandler,
			IdentityConfirmHandler confirmHandler, EntityMergeHandler entityMergeHandler,
			IdentitiesTreeGrid identitiesTable, AttributeSupport attributeSupport, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.identitiesTable = identitiesTable;
		this.attrsMan = attrsMan;
		this.notificationPresenter = notificationPresenter;

		try
		{
			AttributeType nameAt = attributeSupport
					.getAttributeTypeWithSingeltonMetadata(EntityNameMetadataProvider.NAME);
			this.entityNameAttribute = nameAt == null ? null : nameAt.getName();
		} catch (EngineException e)
		{
			log.error("Can not determine name attribute", e);
		}
		
		main = new VerticalLayout();

		toolbar = new Toolbar<>();
		toolbar.setWidthFull();
		toolbar.addHamburger(getHamburgerMenu(removeFromGroupHandler, addToGroupHandler,
				entityCreationDialogHandler, deleteEntityHandler, confirmationResendHandler,
				confirmHandler, entityMergeHandler), Alignment.END);
		toolbar.addSearch(getSearchField());

		filtersBar = new HorizontalLayout();
		filtersBar.add(new Span(msg.getMessage("Identities.filters")));
		filtersBar.setVisible(false);
		filtersBar.addClassName(SMALL_GAP.getName());

		main.setPadding(false);
		main.setClassName(SMALL_GAP.getName());

		main.setSizeFull();

		setSizeFull();
		add(new H5(msg.getMessage("Identities.caption")));
		add(main);

		bus = WebSession.getCurrent().getEventBus();

		bus.addListener(event -> setGroup(event.group()), GroupChangedEvent.class);
		
		bus.addListener(event -> refreshGroupAndSelectIfNeeded(), RefreshAndSelectEvent.class);
		
		bus.addListener(event -> setGroup(identitiesTable.getGroup()), CredentialRequirementChangedEvent.class);
		bus.addListener(event -> {
			if (event.updatedExisting())
				setGroup(identitiesTable.getGroup());
		}, CredentialDefinitionChangedEvent.class);

		bus.addListener(event -> setGroup(identitiesTable.getGroup()), AttributeTypesUpdatedEvent.class);

		bus.addListener(event -> {
			Set<String> interestingCurrent = identitiesTable.getAttributeColumns(false);
			interestingCurrent.add(entityNameAttribute);
			Group curGroup = identitiesTable.getGroup();
			if (interestingCurrent.contains(event.attributeName()) &&
					curGroup.getPathEncoded().equals(event.group()))
			{
				setGroupWithSelectionSave(curGroup);
				return;
			}
			if (curGroup.isTopLevel() && curGroup.getPathEncoded().equals(event.group()))
			{
				Set<String> interestingRoot = identitiesTable.getAttributeColumns(true);
				if (interestingRoot.contains(event.attributeName()))
				{
					setGroupWithSelectionSave(curGroup);
				}
			}		
		}, AttributeChangedEvent.class);

		setGroup(null);
	}

	private void setGroupWithSelectionSave(Group group)
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
		searchText = new SearchField(msg.getMessage("search"), event -> {
			if (fastSearchFilter != null)
				identitiesTable.removeFilter(fastSearchFilter);
			if (event.isEmpty())
				return;
			fastSearchFilter = e -> e.anyFieldContains(event, identitiesTable.getVisibleColumnIds());
			identitiesTable.addFilter(fastSearchFilter);
		});

		return searchText;
	}

	private ActionMenuWithHandlerSupport<IdentityEntry> getHamburgerMenu(RemoveFromGroupHandler removeFromGroupHandler,
			AddToGroupHandler addToGroupHandler, EntityCreationHandler entityCreationDialogHandler,
			DeleteEntityHandler deleteEntityHandler,
			IdentityConfirmationResendHandler confirmationResendHandler,
			IdentityConfirmHandler confirmHandler, EntityMergeHandler entityMergeHandler)
	{
		ActionMenuWithHandlerSupport<IdentityEntry> hamburgerMenu = new ActionMenuWithHandlerSupport<>();
		identitiesTable.addSelectionListener(hamburgerMenu.getSelectionListener());

		SingleActionHandler<IdentityEntry> entityCreationAction = entityCreationDialogHandler
				.getAction(identitiesTable::getGroup, added -> refresh());
		hamburgerMenu.addActionHandler(entityCreationAction);
		hamburgerMenu.addItem(new Hr()).addClassName("hr");

		SingleActionHandler<IdentityEntry> addToGroupAction = addToGroupHandler.getAction();
		hamburgerMenu.addActionHandler(addToGroupAction);

		SingleActionHandler<IdentityEntry> removeFromGroupAction = removeFromGroupHandler
				.getAction(identitiesTable::getGroupPath, this::refresh);
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

		hamburgerMenu.addItem(new Hr()).addClassName("hr");

		hamburgerMenu.addItem(new MenuButton(msg.getMessage("Identities.addFilter"), VaadinIcon.FUNNEL), c -> {
			List<String> columnIds = identitiesTable.getColumnIds();
			new AddFilterDialog(msg, columnIds, this::addFilterInfo)
					.open();
		});

		hamburgerMenu.addItem(new MenuButton(msg.getMessage("Identities.addAttributes"), VaadinIcon.PLUS_SQUARE_O), c -> new AddAttributeColumnDialog(msg, attrsMan,
				identitiesTable::addAttributeColumn, notificationPresenter).open());

		hamburgerMenu.addItem(new MenuButton(msg.getMessage("Identities.removeAttributes"), VaadinIcon.MINUS_SQUARE_O),
				c -> {
					Set<String> alreadyUsedRoot = identitiesTable.getAttributeColumns(true);
					Set<String> alreadyUsedCurrent = identitiesTable.getAttributeColumns(false);
					new RemoveAttributeColumnDialog(msg, alreadyUsedRoot, alreadyUsedCurrent,
							identitiesTable.getGroupPath(),
							(attributeType, group) -> identitiesTable
									.removeAttributeColumn(group, attributeType))
											.open();
				});

		MenuItem showTargeted = hamburgerMenu.addItem(msg.getMessage("Identities.showTargeted"), c -> {
			try
			{
				identitiesTable.setShowTargeted(c.getSource().isChecked());
			} catch (EngineException e)
			{
				setIdProblem(IdentitiesPanel.this.identitiesTable.getGroup(), e);
			}
		});
		showTargeted.setCheckable(true);
		showTargeted.setChecked(identitiesTable.isShowTargeted());

		MenuItem mode = hamburgerMenu.addItem(msg.getMessage("Identities.mode"), c -> identitiesTable.setMode(c.getSource().isChecked()));

		mode.setCheckable(true);
		mode.setChecked(identitiesTable.isGroupByEntity());
		return hamburgerMenu;
	}

	private void refreshGroupAndSelectIfNeeded()
	{
		Group currentGroup = identitiesTable.getGroup();
		setGroup(currentGroup == null ? new Group("/") : currentGroup);
	}

	
	private void setGroup(Group group)
	{
		removeAll();
		main.removeAll();
		identitiesTable.clearFilters();
		searchText.clear();

		if (group == null)
		{
			try
			{
				identitiesTable.showGroup(null);
			} catch (EngineException e)
			{
				log.trace(e);
				// ignored, shouldn't happen anyway
			}
			add(new H5(msg.getMessage("Identities.captionNoGroup")));
			add(new HorizontalLayout(VaadinIcon.EXCLAMATION_CIRCLE_O.create(),
					new Span(msg.getMessage("Identities.noGroupSelected"))));
			return;
		}
		try
		{
			identitiesTable.showGroup(group);
			identitiesTable.setVisible(true);
			main.add(new Html("<h5>" + msg.getMessage("Identities.caption", group.getDisplayedNameShort(msg).getValue(msg)) + "</h5>"));
			main.add(toolbar, filtersBar, identitiesTable);
			add(main);
		} catch (AuthorizationException e)
		{
			add(new HorizontalLayout(VaadinIcon.EXCLAMATION_CIRCLE_O.create(), new ErrorLabel(msg.getMessage("Identities.noReadAuthz", group))));
		} catch (Exception e)
		{
			setIdProblem(group, e);
		}
	}

	private void setIdProblem(Group group, Exception e)
	{
		log.error("Problem retrieving group contents of " + group.getPathEncoded(), e);
		add(new HorizontalLayout(VaadinIcon.EXCLAMATION_CIRCLE_O.create(), new ErrorLabel(msg.getMessage("Identities.internalError", e.toString()))));
	}

	private void refresh()
	{
		bus.fireEvent(new GroupChangedEvent(identitiesTable.getGroup()));
	}

	private void addFilterInfo(EntityFilter filter, String description)
	{
		identitiesTable.addFilter(filter);
		filtersBar.add(new FilterInfo(description, filter));
		filtersBar.setVisible(true);
	}

	private class FilterInfo extends HorizontalLayout
	{
		public FilterInfo(String description, EntityFilter filter)
		{
			Span info = new Span(description);
			Icon remove = VaadinIcon.TRASH.create();
			remove.addClassName(POINTER.getName());
			remove.addClickListener(event -> {
				identitiesTable.removeFilter(filter);
				filtersBar.remove(FilterInfo.this);
				if (filtersBar.getComponentCount() == 1)
					filtersBar.setVisible(false);
			});
			add(info, remove);
		}
	}
}
