/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webadmin.credentials.CredentialDefinitionChangedEvent;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.webadmin.attribute.AttributeChangedEvent;
import pl.edu.icm.unity.webadmin.attributetype.AttributeTypesUpdatedEvent;
import pl.edu.icm.unity.webadmin.credreq.CredentialRequirementChangedEvent;
import pl.edu.icm.unity.webadmin.groupbrowser.GroupChangedEvent;
import pl.edu.icm.unity.webadmin.identities.IdentityCreationDialog.IdentityCreationDialogHandler;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.EntityWithLabel;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ErrorComponent.Level;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.common.credentials.CredentialsChangeDialog;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Component wrapping {@link IdentitiesTable}. Allows to configure its mode, 
 * feeds it with data to be visualised etc.
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class IdentitiesComponent extends SafePanel
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, IdentitiesComponent.class);
	private UnityMessageSource msg;
	private VerticalLayout main;
	private IdentitiesGrid identitiesTable;
	private HorizontalLayout filtersBar;
	private EntityFilter fastSearchFilter;
	private EventsBus bus;
	private ObjectFactory<CredentialsChangeDialog> credentialChangeDialogFactory;
	
	@Autowired
	public IdentitiesComponent(UnityMessageSource msg, AttributeTypeManagement attrsMan,
			EntityAttributeClassHandler entityAttributeClassHandler,
			RemoveFromGroupHandler removeFromGroupHandler,
			AddToGroupHandler addToGroupHandler,
			EntityCreationHandler entityCreationDialogHandler,
			IdentityCreationDialogHandler identityCreationDialogHanlder,
			DeleteEntityHandler deleteEntityHandler,
			DeleteIdentityHandler deleteIdentityHandler,
			IdentityConfirmationResendHandler confirmationResendHandler,
			IdentityConfirmHandler confirmHandler,
			ChangeEntityStateHandler changeEntityStateHandler,
			ChangeCredentialRequirementHandler credentialRequirementHandler,
			EntityDetailsHandler entityDetailsHandler,
			EntityMergeHandler entityMergeHandler,
			IdentitiesGrid identitiesTable, 
			ObjectFactory<CredentialsChangeDialog> credentialChangeDialogFactory)
	{
		this.msg = msg;
		this.identitiesTable = identitiesTable;
		this.credentialChangeDialogFactory = credentialChangeDialogFactory;

		main = new VerticalLayout();
		main.addStyleName(Styles.visibleScroll.toString());
		
		HorizontalLayout topBar = new HorizontalLayout();
		topBar.setMargin(false);
		topBar.setSpacing(false);
		
		final CheckBox mode = new CheckBox(msg.getMessage("Identities.mode"));
		
		mode.setValue(IdentitiesComponent.this.identitiesTable.isGroupByEntity());
		mode.addStyleName(Styles.vSmall.toString());
		mode.addStyleName(Styles.verticalAlignmentMiddle.toString());
		mode.addStyleName(Styles.horizontalMarginSmall.toString());
		
		final CheckBox showTargeted = new CheckBox(msg.getMessage("Identities.showTargeted"));
		showTargeted.setValue(IdentitiesComponent.this.identitiesTable.isShowTargeted());
		showTargeted.addStyleName(Styles.vSmall.toString());
		showTargeted.addStyleName(Styles.verticalAlignmentMiddle.toString());
		showTargeted.addStyleName(Styles.horizontalMarginSmall.toString());

		Toolbar<IdentityEntry> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		identitiesTable.addSelectionListener(toolbar.getSelectionListener());
		toolbar.addStyleName(Styles.floatRight.toString());
		toolbar.addStyleName(Styles.verticalAlignmentMiddle.toString());
		toolbar.addStyleName(Styles.horizontalMarginSmall.toString());

		HamburgerMenu<IdentityEntry> hamburgerMenu= new HamburgerMenu<>();
		identitiesTable.addSelectionListener(hamburgerMenu.getSelectionListener());
		
		filtersBar = new HorizontalLayout();
		filtersBar.addComponent(new Label(msg.getMessage("Identities.filters")));
		filtersBar.setMargin(false);
		filtersBar.setSpacing(false);
		filtersBar.setVisible(false);
	
		HorizontalLayout searchWrapper = new HorizontalLayout();
		searchWrapper.setSpacing(true);
		searchWrapper.setMargin(false);
		searchWrapper.addStyleName(Styles.verticalAlignmentMiddle.toString());
		searchWrapper.addStyleName(Styles.horizontalMarginSmall.toString());

		Label searchL = new Label(msg.getMessage("Identities.searchCaption"));
		Label spacer = new Label();
		spacer.setWidth(4, Unit.EM);
		final TextField searchText = new TextField();
		searchText.addStyleName(Styles.vSmall.toString());
		searchText.setWidth(8, Unit.EM);
		searchWrapper.addComponents(spacer, searchL, searchText);
		searchWrapper.setComponentAlignment(searchL, Alignment.MIDDLE_RIGHT);
		searchWrapper.setComponentAlignment(searchText, Alignment.MIDDLE_LEFT);
		
		searchText.addValueChangeListener(event -> 
		{
			String searched = event.getValue();
			if (fastSearchFilter != null)
				identitiesTable.removeFilter(fastSearchFilter);
			if (searched.isEmpty())
				return;
			fastSearchFilter = e -> e.anyFieldContains(searched, 
					identitiesTable.getVisibleColumnIds());
			identitiesTable.addFilter(fastSearchFilter);
		});

		SingleActionHandler<IdentityEntry> refreshAction = getRefreshAction();
		identitiesTable.addActionHandler(refreshAction);

		SingleActionHandler<IdentityEntry> entityDetailsAction = entityDetailsHandler
				.getShowEntityAction();
		identitiesTable.addActionHandler(entityDetailsAction);

		SingleActionHandler<IdentityEntry> entityCreationAction = entityCreationDialogHandler
				.getAction(identitiesTable::getGroup, added -> refresh());
		identitiesTable.addActionHandler(entityCreationAction);

		SingleActionHandler<IdentityEntry> addToGroupAction = addToGroupHandler.getAction();
		identitiesTable.addActionHandler(addToGroupAction);

		SingleActionHandler<IdentityEntry> removeFromGroupAction = removeFromGroupHandler
				.getAction(identitiesTable::getGroup, this::refresh);
		identitiesTable.addActionHandler(removeFromGroupAction);

		SingleActionHandler<IdentityEntry> deleteEntityAction = deleteEntityHandler
				.getAction(identitiesTable::removeEntity);
		identitiesTable.addActionHandler(deleteEntityAction);

		SingleActionHandler<IdentityEntry> identityCreationAction = identityCreationDialogHanlder
				.getAction(added -> refresh());

		identitiesTable.addActionHandler(identityCreationAction);

		SingleActionHandler<IdentityEntry> deleteIdentityAction = deleteIdentityHandler
				.getAction(identitiesTable::removeIdentity, this::refresh);
		identitiesTable.addActionHandler(deleteIdentityAction);

		SingleActionHandler<IdentityEntry> changeEntityStateAction = changeEntityStateHandler
				.getAction(this::refresh);
		identitiesTable.addActionHandler(changeEntityStateAction);

		SingleActionHandler<IdentityEntry> changeCredentialAction= getChangeCredentialAction();
		identitiesTable.addActionHandler(changeCredentialAction);

		SingleActionHandler<IdentityEntry> credentialRequirementAction = credentialRequirementHandler
				.getAction(this::refresh);
		identitiesTable.addActionHandler(credentialRequirementAction);

		SingleActionHandler<IdentityEntry> entityAttributeAction = entityAttributeClassHandler
				.getAction(this::refresh, identitiesTable::getGroup);
		identitiesTable.addActionHandler(entityAttributeAction);

		SingleActionHandler<IdentityEntry> entityMergeAction = entityMergeHandler
				.getAction(identitiesTable::getGroup);
		identitiesTable.addActionHandler(entityMergeAction);

		SingleActionHandler<IdentityEntry> confirmationResendAction = confirmationResendHandler
				.getAction();
		identitiesTable.addActionHandler(confirmationResendAction);

		SingleActionHandler<IdentityEntry> confirmAction = confirmHandler.getAction(this::refresh);
		identitiesTable.addActionHandler(confirmAction);

		toolbar.addActionHandler(entityDetailsAction);
		toolbar.addActionHandler(entityCreationAction);
		toolbar.addActionHandler(removeFromGroupAction);
		toolbar.addActionHandler(deleteEntityAction);
		toolbar.addHamburger(hamburgerMenu);
		hamburgerMenu.addActionHandler(refreshAction);
		hamburgerMenu.addActionHandler(identityCreationAction);
		hamburgerMenu.addActionHandler(addToGroupAction);
		hamburgerMenu.addActionHandler(deleteIdentityAction);
		hamburgerMenu.addActionHandler(changeEntityStateAction);
		hamburgerMenu.addActionHandler(changeCredentialAction);
		hamburgerMenu.addActionHandler(credentialRequirementAction);
		hamburgerMenu.addActionHandler(entityAttributeAction);
		hamburgerMenu.addActionHandler(entityMergeAction);
		hamburgerMenu.addActionHandler(confirmationResendAction);
		hamburgerMenu.addActionHandler(confirmAction);

		hamburgerMenu.addSeparator();

		hamburgerMenu.addItem(msg.getMessage("Identities.addFilter"),
				Images.addFilter.getResource(), c -> {
					List<String> columnIds = identitiesTable.getColumnIds();
					new AddFilterDialog(msg, columnIds,
							(filter, description) -> addFilterInfo(
									filter, description))
											.show();
				});

		hamburgerMenu.addItem(msg.getMessage("Identities.addAttributes"),
				Images.addColumn.getResource(), c -> {
					new AddAttributeColumnDialog(msg, attrsMan,
							(attributeType, group) -> identitiesTable
									.addAttributeColumn(
											attributeType,
											group)).show();
				});

		hamburgerMenu.addItem(msg.getMessage("Identities.removeAttributes"),
				Images.removeColumn.getResource(), c -> {
					Set<String> alreadyUsedRoot = identitiesTable
							.getAttributeColumns(true);
					Set<String> alreadyUsedCurrent = identitiesTable
							.getAttributeColumns(false);
					new RemoveAttributeColumnDialog(msg, alreadyUsedRoot,
							alreadyUsedCurrent,
							identitiesTable.getGroup(),
							(attributeType, group) -> identitiesTable
									.removeAttributeColumn(
											group,
											attributeType)).show();
				});

		
		Label sep = new Label("");
		topBar.addComponents(mode, showTargeted, searchWrapper,sep, toolbar);
		topBar.setExpandRatio(sep, 2);
			
		topBar.setWidth(100, Unit.PERCENTAGE);

		mode.addValueChangeListener(event -> identitiesTable.setMode(mode.getValue()));

		showTargeted.addValueChangeListener(event -> {
			try
			{
				identitiesTable.setShowTargeted(showTargeted.getValue());
			} catch (EngineException e)
			{
				setIdProblem(IdentitiesComponent.this.identitiesTable.getGroup(),
						e);
			}
		});

		
		main.addComponents(topBar, filtersBar, identitiesTable);
		main.setExpandRatio(identitiesTable, 1.0f);
		main.setMargin(false);
		main.setSizeFull();
		
		setSizeFull();
		setContent(main);
		setStyleName(Styles.vPanelLight.toString());
		setCaption(msg.getMessage("Identities.caption"));

		bus = WebSession.getCurrent().getEventBus();
		
		bus.addListener(event -> setGroup(event.getGroup()), GroupChangedEvent.class);

		bus.addListener(event -> setGroup(identitiesTable.getGroup()), 
				CredentialRequirementChangedEvent.class);
		
		bus.addListener(event -> 
		{
			if (event.isUpdatedExisting())
				setGroup(identitiesTable.getGroup());
		}, CredentialDefinitionChangedEvent.class);

		bus.addListener(event -> setGroup(identitiesTable.getGroup()), 
				AttributeTypesUpdatedEvent.class);
		
		bus.addListener(event ->
		{
			Set<String> interestingCurrent = identitiesTable.getAttributeColumns(false);
			String curGroup = identitiesTable.getGroup();
			if (interestingCurrent.contains(event.getAttributeName()) && curGroup.equals(event.getGroup()))
			{
				setGroup(curGroup);
				return;
			}
			if (curGroup.equals("/") && curGroup.equals(event.getGroup()))
			{
				Set<String> interestingRoot = identitiesTable.getAttributeColumns(true);
				if (interestingRoot.contains(event.getAttributeName()))
				{
					setGroup(curGroup);
					return;
				}
			}
		}, AttributeChangedEvent.class);
		
		setGroup(null);
	}

	
	static String getConfirmTextForIdentitiesNodes(UnityMessageSource msg, Set<IdentityEntry> selection)
	{
		Collection<String> ids = new ArrayList<>();
		for (IdentityEntry o: selection)
			ids.add(o.getSourceIdentity().toString());		
		return MessageUtils.createConfirmFromStrings(msg, ids);
	}
	
	private void addFilterInfo(EntityFilter filter, String description)
	{
		identitiesTable.addFilter(filter);
		filtersBar.addComponent(new FilterInfo(description, filter));
		filtersBar.setVisible(true);
	}
	
	private void setGroup(String group)
	{
		if (group == null)
		{
			try
			{
				identitiesTable.showGroup(null);
			} catch (EngineException e)
			{
				//ignored, shouldn't happen anyway
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
	
	private class FilterInfo extends HorizontalLayout
	{
		public FilterInfo(String description, EntityFilter filter)
		{
			Label info = new Label(description);
			Button remove = new Button();
			remove.addStyleName(Styles.vButtonLink.toString());
			remove.setIcon(Images.delete.getResource());
			remove.addClickListener(event -> 
			{
				identitiesTable.removeFilter(filter);
				filtersBar.removeComponent(FilterInfo.this);
				if (filtersBar.getComponentCount() == 1)
					filtersBar.setVisible(false);
			});
			addComponents(info, HtmlTag.hspaceEm(1), remove);
			setMargin(new MarginInfo(false, false, false, true));
		}
	}
	
	private void refresh()
	{
		bus.fireEvent(new GroupChangedEvent(identitiesTable.getGroup()));
	}
	
	private SingleActionHandler<IdentityEntry> getRefreshAction()
	{
		return SingleActionHandler.builder4Refresh(msg, IdentityEntry.class)
				.withHandler(selection -> refresh())
				.build();
	}

	private SingleActionHandler<IdentityEntry> getChangeCredentialAction()
	{
		return SingleActionHandler.builder(IdentityEntry.class)
				.withCaption(msg.getMessage("Identities.changeCredentialAction"))
				.withIcon(Images.key.getResource())
				.withHandler(this::showChangeCredentialDialog)
				.build();
	}

	private void showChangeCredentialDialog(Set<IdentityEntry> selection)
	{
		EntityWithLabel entity = selection.iterator().next().getSourceEntity();
		credentialChangeDialogFactory.getObject().
			init(entity.getEntity().getId(), false, changed -> 
			{
				if (changed)
					refresh();
			}).show();
	}
}
