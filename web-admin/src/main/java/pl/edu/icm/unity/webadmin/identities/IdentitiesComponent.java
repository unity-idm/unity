/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.webadmin.attribute.AttributeChangedEvent;
import pl.edu.icm.unity.webadmin.credentials.CredentialDefinitionChangedEvent;
import pl.edu.icm.unity.webadmin.credreq.CredentialRequirementChangedEvent;
import pl.edu.icm.unity.webadmin.groupbrowser.GroupChangedEvent;
import pl.edu.icm.unity.webadmin.identities.AddAttributeColumnDialog.Callback;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventListener;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ErrorComponent;
import pl.edu.icm.unity.webui.common.ErrorComponent.Level;
import pl.edu.icm.unity.webui.common.HtmlTag;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SafePanel;
import pl.edu.icm.unity.webui.common.Toolbar;

import com.vaadin.data.Container;
import com.vaadin.data.Container.Filter;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.filter.Or;
import com.vaadin.data.util.filter.SimpleStringFilter;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

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
	private static final List<Long> EMPTY_LIST = new ArrayList<Long>(0);
	private UnityMessageSource msg;
	private VerticalLayout main;
	private GroupsManagement groupsManagement;
	private IdentitiesTable identitiesTable;
	private HorizontalLayout filtersBar;
	private Or fastSearchFilter;
	
	@Autowired
	public IdentitiesComponent(final UnityMessageSource msg, GroupsManagement groupsManagement,
			final AttributesManagement attrsMan, final IdentitiesTable identitiesTable)
	{
		this.msg = msg;
		this.groupsManagement = groupsManagement;
		this.identitiesTable = identitiesTable;

		main = new VerticalLayout();
		
		HorizontalLayout topBar = new HorizontalLayout();
		topBar.setSpacing(true);
		final CheckBox mode = new CheckBox(msg.getMessage("Identities.mode"));
		mode.setImmediate(true);
		mode.setValue(IdentitiesComponent.this.identitiesTable.isGroupByEntity());
		
		final CheckBox showTargeted = new CheckBox(msg.getMessage("Identities.showTargeted"));
		showTargeted.setImmediate(true);
		showTargeted.setValue(IdentitiesComponent.this.identitiesTable.isShowTargeted());
		
		Toolbar toolbar = new Toolbar(identitiesTable, Orientation.HORIZONTAL);
		
		filtersBar = new HorizontalLayout();
		filtersBar.addComponent(new Label(msg.getMessage("Identities.filters")));
		filtersBar.setVisible(false);
		
		Button addAttributes = new Button();
		addAttributes.setDescription(msg.getMessage("Identities.addAttributes"));
		addAttributes.setIcon(Images.addColumn.getResource());
		addAttributes.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				new AddAttributeColumnDialog(IdentitiesComponent.this.msg, 
						attrsMan, new Callback()
						{
							@Override
							public void onChosen(String attributeType, String group)
							{
								IdentitiesComponent.this.identitiesTable.
									addAttributeColumn(attributeType, group);
							}
						}).show(); 
			}
		});
		
		Button removeAttributes = new Button();
		removeAttributes.setDescription(msg.getMessage("Identities.removeAttributes"));
		removeAttributes.setIcon(Images.removeColumn.getResource());
		removeAttributes.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				Set<String> alreadyUsedRoot = 
						IdentitiesComponent.this.identitiesTable.getAttributeColumns(true);
				Set<String> alreadyUsedCurrent = 
						IdentitiesComponent.this.identitiesTable.getAttributeColumns(false);
				new RemoveAttributeColumnDialog(IdentitiesComponent.this.msg, alreadyUsedRoot,
						alreadyUsedCurrent, IdentitiesComponent.this.identitiesTable.getGroup(),
						new RemoveAttributeColumnDialog.Callback()
						{
							@Override
							public void onChosen(String attributeType, String group)
							{
								IdentitiesComponent.this.identitiesTable.
									removeAttributeColumn(group, attributeType);
							}
						}).show(); 
			}
		});
		
		Button addFilter = new Button();
		addFilter.setDescription(msg.getMessage("Identities.addFilter"));
		addFilter.setIcon(Images.addFilter.getResource());
		addFilter.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				Collection<?> props = IdentitiesComponent.this.identitiesTable.getContainerPropertyIds();
				new AddFilterDialog(IdentitiesComponent.this.msg, props, 
						new AddFilterDialog.Callback()
						{
							@Override
							public void onConfirm(Filter filter, String description)
							{
								addFilterInfo(filter, description);
							}
						}).show(); 
			}
		});
		
		
		Button savePreferences = new Button();
		savePreferences.setDescription(msg.getMessage("Identities.savePreferences"));
		savePreferences.setIcon(Images.save.getResource());
		savePreferences.addClickListener(new ClickListener()
		{
			
			@Override
			public void buttonClick(ClickEvent event)
			{
				IdentitiesComponent.this.identitiesTable.savePreferences();
				
			}
		});
		HorizontalLayout searchWrapper = new HorizontalLayout();
		searchWrapper.setSpacing(false);
		searchWrapper.setMargin(false);
		
		FormLayout searchToolbar = new FormLayout();
		searchToolbar.setSpacing(false);
		searchToolbar.setMargin(false);
		
		final TextField searchText = new TextField(
				msg.getMessage("Identities.searchCaption"));
		searchToolbar.addComponent(searchText);
		searchWrapper.addComponent(searchToolbar);
		
		searchText.setImmediate(true);
		searchText.addTextChangeListener(new TextChangeListener()
		{
			@Override
			public void textChange(TextChangeEvent event)
			{
				Collection<?> props = IdentitiesComponent.this.identitiesTable
						.getContainerPropertyIds();
				ArrayList<Container.Filter> filters = new ArrayList<Container.Filter>();
				String searchText = event.getText();
				if (fastSearchFilter != null)
					identitiesTable.removeFilter(fastSearchFilter);
				if (searchText.isEmpty())
					return;
				for (Object colIdRaw : props)
				{
					String colId = (String) colIdRaw;
					if (IdentitiesComponent.this.identitiesTable.isColumnCollapsed(colId))
						continue;
					Filter filter = new SimpleStringFilter(colId, searchText, true, false);
					filters.add(filter);
				}
				if (filters.size() < 1)
					return;
				Filter[] orFillters = filters.toArray(new Filter[filters.size()]);
				fastSearchFilter = new Or(orFillters);
				identitiesTable.addFilter(fastSearchFilter);
			}
		});

		Label spacer = new Label();
		spacer.setSizeFull();
		toolbar.addSeparator();
		toolbar.addActionHandlers(identitiesTable.getActionHandlers());
		toolbar.addSeparator();
		toolbar.addButtons(addFilter, addAttributes, removeAttributes, savePreferences);
		topBar.addComponents(mode, showTargeted, spacer, searchWrapper, toolbar);
		topBar.setExpandRatio(spacer, 2f);
		topBar.setWidth(100, Unit.PERCENTAGE);
		
		mode.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				IdentitiesComponent.this.identitiesTable.setMode(mode.getValue());
			}
		});
		
		showTargeted.addValueChangeListener(new ValueChangeListener()
		{
			
			@Override
			public void valueChange(ValueChangeEvent event)
			{
					try
					{
						IdentitiesComponent.this.identitiesTable.setShowTargeted(showTargeted.getValue());
					} catch (EngineException e)
					{
						setIdProblem(IdentitiesComponent.this.identitiesTable.getGroup(), e);
					}	
			}
		});

		
		main.addComponents(topBar, filtersBar, identitiesTable);
		main.setExpandRatio(identitiesTable, 1.0f);
		main.setSpacing(true);
		main.setSizeFull();
		
		setSizeFull();
		setContent(main);
		setStyleName(Reindeer.PANEL_LIGHT);
		setCaption(msg.getMessage("Identities.caption"));

		EventsBus bus = WebSession.getCurrent().getEventBus();
		bus.addListener(new EventListener<GroupChangedEvent>()
		{
			@Override
			public void handleEvent(GroupChangedEvent event)
			{
				setGroup(event.getGroup());
			}
		}, GroupChangedEvent.class);
		
		bus.addListener(new EventListener<CredentialRequirementChangedEvent>()
		{
			@Override
			public void handleEvent(CredentialRequirementChangedEvent event)
			{
				setGroup(IdentitiesComponent.this.identitiesTable.getGroup());
			}
		}, CredentialRequirementChangedEvent.class);
		bus.addListener(new EventListener<CredentialDefinitionChangedEvent>()
		{
			@Override
			public void handleEvent(CredentialDefinitionChangedEvent event)
			{
				if (event.isUpdatedExisting())
					setGroup(IdentitiesComponent.this.identitiesTable.getGroup());
			}
		}, CredentialDefinitionChangedEvent.class);
		bus.addListener(new EventListener<AttributeChangedEvent>()
		{
			@Override
			public void handleEvent(AttributeChangedEvent event)
			{
				Set<String> interestingCurrent = 
						IdentitiesComponent.this.identitiesTable.getAttributeColumns(false);
				String curGroup = IdentitiesComponent.this.identitiesTable.getGroup();
				if (interestingCurrent.contains(event.getAttributeName()) && curGroup.equals(event.getGroup()))
				{
					setGroup(curGroup);
					return;
				}
				if (curGroup.equals("/") && curGroup.equals(event.getGroup()))
				{
					Set<String> interestingRoot = 
						IdentitiesComponent.this.identitiesTable.getAttributeColumns(true);
					if (interestingRoot.contains(event.getAttributeName()))
					{
						setGroup(curGroup);
						return;
					}
				}
			}
		}, AttributeChangedEvent.class);
		setGroup(null);
	}
	
	private void addFilterInfo(Filter filter, String description)
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
				identitiesTable.setInput(null, EMPTY_LIST);
			} catch (EngineException e)
			{
				//ignored, shouldn't happen anyway
			}
			setProblem(msg.getMessage("Identities.noGroupSelected"), Level.warning);
			return;
		}
		try
		{
			GroupContents contents = groupsManagement.getContents(group, GroupContents.MEMBERS);
			identitiesTable.setInput(group, contents.getMembers());
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
		public FilterInfo(String description, final Filter filter)
		{
			Label info = new Label(description);
			Button remove = new Button();
			remove.setStyleName(Reindeer.BUTTON_SMALL);
			remove.setIcon(Images.delete.getResource());
			remove.addClickListener(new ClickListener()
			{
				@Override
				public void buttonClick(ClickEvent event)
				{
					identitiesTable.removeFilter(filter);
					filtersBar.removeComponent(FilterInfo.this);
					if (filtersBar.getComponentCount() == 1)
						filtersBar.setVisible(false);
				}
			});
			addComponents(info, HtmlTag.hspaceEm(1), remove);
			setMargin(new MarginInfo(false, false, false, true));
		}
	}
}
