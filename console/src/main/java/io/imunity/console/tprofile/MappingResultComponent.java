/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.tprofile;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.translation.in.MappedAttribute;
import pl.edu.icm.unity.engine.api.translation.in.MappedGroup;
import pl.edu.icm.unity.engine.api.translation.in.MappedIdentity;
import pl.edu.icm.unity.engine.api.translation.in.MappingResult;

import java.util.Collections;
import java.util.List;

/**
 * Component that displays Mapping Result.
 */
public class MappingResultComponent extends VerticalLayout
{

	private VerticalLayout mappingResultWrap;
	private VerticalLayout groupsWrap;
	private Span groupsTitleLabel;
	private VerticalLayout attrsWrap;
	private Grid<MappedAttribute> attrsTable;
	private Grid<MappedGroup> groupsTable;
	private Span attrsTitleLabel;
	private VerticalLayout idsWrap;
	private Grid<MappedIdentity> idsTable;
	private Span idsTitleLabel;
	private HorizontalLayout titleWrap;
	private Span noneLabel;
	private Html titleLabel;
	private final MessageSource msg;
	/**
	 * The constructor should first build the main layout, set the
	 * composition root and then do any custom initialization.
	 * <p>
	 * The constructor will not be automatically regenerated by the
	 * visual editor.
	 */
	public MappingResultComponent(MessageSource msg) 
	{
		this.msg = msg;

		add(buildMainLayout());
		
		setVisible(false);
		initLabels();
		initTables();
	}

	private void initLabels() 
	{
		titleLabel.setHtmlContent("<div>" + msg.getMessage("MappingResultComponent.title") + "</div>" );
		idsTitleLabel.setText(msg.getMessage("MappingResultComponent.idsTitle"));
		attrsTitleLabel.setText(msg.getMessage("MappingResultComponent.attrsTitle"));
		groupsTitleLabel.setText(msg.getMessage("MappingResultComponent.groupsTitle"));
		noneLabel.setText(msg.getMessage("MappingResultComponent.none"));
	}
	
	private void initTables()
	{
		idsTable.addColumn(MappedIdentity::getMode)
				.setHeader(msg.getMessage("MappingResultComponent.mode"));
		idsTable.addColumn(v -> v.getIdentity().getTypeId())
				.setHeader(msg.getMessage("MappingResultComponent.idsTable.type"));
		idsTable.addColumn(v -> v.getIdentity().getValue())
				.setHeader(msg.getMessage("MappingResultComponent.idsTable.value"));
		attrsTable.addColumn(MappedAttribute::getMode)
				.setHeader(msg.getMessage("MappingResultComponent.mode"));
		attrsTable.addColumn(v -> v.getAttribute().getName())
				.setHeader(msg.getMessage("MappingResultComponent.attrsTable.name"));
		attrsTable.addColumn(v -> v.getAttribute().getValues())
				.setHeader(msg.getMessage("MappingResultComponent.attrsTable.value"));
		groupsTable.addColumn(MappedGroup::getCreateIfMissing)
				.setHeader(msg.getMessage("MappingResultComponent.mode"));
		groupsTable.addColumn(MappedGroup::getGroup)
				.setHeader(msg.getMessage("MappingResultComponent.groupsTable.group"));
	}
	
	public void displayMappingResult(MappingResult mappingResult)
	{
		if (mappingResult == null 
				|| (mappingResult.getIdentities().isEmpty()
					&& mappingResult.getAttributes().isEmpty()
					&& mappingResult.getGroups().isEmpty()))
		{
			displayItsTables(Collections.emptyList());
			displayAttrsTable(Collections.emptyList());
			displayGroups(Collections.emptyList());
			noneLabel.setVisible(true);
		} else
		{
			displayItsTables(mappingResult.getIdentities());
			displayAttrsTable(mappingResult.getAttributes());
			displayGroups(mappingResult.getGroups());
			noneLabel.setVisible(false);
		}
		setVisible(true);
	}

	public void displayMappingResult(MappingResult mappingResult, String inputTranslationProfile)
	{
		titleLabel.setHtmlContent("<div>" + msg.getMessage("DryRun.MappingResultComponent.title", inputTranslationProfile) + "</div>");
		noneLabel.setText("");
		displayMappingResult(mappingResult);
	}

	private void displayItsTables(List<MappedIdentity> identities) 
	{
		idsTable.setItems(Collections.emptyList());
		if (identities.isEmpty())
		{
			idsWrap.setVisible(false);
		} else
		{
			idsWrap.setVisible(true);
			idsTable.setItems(identities);		
			idsTable.setAllRowsVisible(true);
		}
	}
	
	private void displayAttrsTable(List<MappedAttribute> attributes) 
	{
		attrsTable.setItems(Collections.emptyList());
		if (attributes.isEmpty())
		{
			attrsWrap.setVisible(false);
		} else
		{
			attrsWrap.setVisible(true);
			attrsTable.setItems(attributes);
			attrsTable.setAllRowsVisible(true);
			
		}
	}
	
	private void displayGroups(List<MappedGroup> groups) 
	{
		groupsTable.setItems(Collections.emptyList());
		if (groups.isEmpty())
		{
			groupsWrap.setVisible(false);
		} else
		{
			groupsWrap.setVisible(true);	
			groupsTable.setItems(groups);
			groupsTable.setAllRowsVisible(true);
		}
	}
	
	private VerticalLayout buildMainLayout() {
		// common part: create layout
		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setSizeFull();
		mainLayout.setMargin(false);
		
		// top-level component properties
		setSizeFull();
		
		// titleWrap
		titleWrap = buildTitleWrap();
		mainLayout.add(titleWrap);
		
		// mappingResultWrap
		mappingResultWrap = buildMappingResultWrap();
		mainLayout.add(mappingResultWrap);

		return mainLayout;
	}

	private HorizontalLayout buildTitleWrap() {
		// common part: create layout
		titleWrap = new HorizontalLayout();
		titleWrap.setWidth("-1px");
		titleWrap.setHeight("-1px");
		titleWrap.setMargin(false);
		//titleWrap.setSpacing(true);
		
		// titleLabel
		titleLabel = new Html("<div></div>");
		titleWrap.add(titleLabel);
		
		// noneLabel
		noneLabel = new Span();
		noneLabel.setWidth("-1px");
		noneLabel.setHeight("-1px");
		noneLabel.setText("Label");
		titleWrap.add(noneLabel);
		
		return titleWrap;
	}

	private VerticalLayout buildMappingResultWrap() {
		// common part: create layout
		mappingResultWrap = new VerticalLayout();
		mappingResultWrap.setHeight("-1px");
		
		// idsWrap
		idsWrap = buildIdsWrap();
		mappingResultWrap.add(idsWrap);
		
		// attrsWrap
		attrsWrap = buildAttrsWrap();
		mappingResultWrap.add(attrsWrap);
		
		// groupsWrap
		groupsWrap = buildGroupsWrap();
		mappingResultWrap.add(groupsWrap);
		
		return mappingResultWrap;
	}

	private VerticalLayout buildIdsWrap() {
		// common part: create layout
		idsWrap = new VerticalLayout();
		idsWrap.setHeight("-1px");
		idsWrap.setMargin(false);
		
		// idsTitleLabel
		idsTitleLabel = new Span();
		idsTitleLabel.setWidth("-1px");
		idsTitleLabel.setHeight("-1px");
		idsTitleLabel.setText("Label");
		idsWrap.add(idsTitleLabel);
		
		// idsTable
		idsTable = new Grid<>();
		idsTable.setWidth("100.0%");
		idsTable.setHeight("-1px");
		idsWrap.add(idsTable);
		
		return idsWrap;
	}

	private VerticalLayout buildAttrsWrap() {
		// common part: create layout
		attrsWrap = new VerticalLayout();
		attrsWrap.setHeight("-1px");
		attrsWrap.setMargin(false);
		
		// attrsTitleLabel
		attrsTitleLabel = new Span();
		attrsTitleLabel.setWidth("-1px");
		attrsTitleLabel.setHeight("-1px");
		attrsTitleLabel.setText("Label");
		attrsWrap.add(attrsTitleLabel);
		
		// attrsTable
		attrsTable = new Grid<>();
		attrsTable.setWidth("100.0%");
		attrsTable.setHeight("-1px");
		attrsWrap.add(attrsTable);
		
		return attrsWrap;
	}

	private VerticalLayout buildGroupsWrap() {
		// common part: create layout
		groupsWrap = new VerticalLayout();
		groupsWrap.setHeight("-1px");
		groupsWrap.setMargin(false);
		
		// groupsTitleLabel
		groupsTitleLabel = new Span();
		groupsTitleLabel.setWidth("-1px");
		groupsTitleLabel.setHeight("-1px");
		groupsTitleLabel.setText("Label");
		groupsWrap.add(groupsTitleLabel);
		
		// groupsLabel
		groupsTable =  new Grid<>();
		groupsTable.setWidth("100.0%");
		groupsTable.setHeight("-1px");
		groupsWrap.add(groupsTable);
		
		return groupsWrap;
	}
}