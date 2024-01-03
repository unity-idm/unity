/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.authentication.facilities;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteGroupMembership;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;

import java.util.Collection;
import java.util.Collections;

class RemotelyAuthenticatedInputComponent extends VerticalLayout
{
	private final MessageSource msg;
	private VerticalLayout mainLayout;
	private VerticalLayout mappingResultWrap;
	private HorizontalLayout groupsWrap;
	private Span groupsLabel;
	private Span groupsTitleLabel;
	private VerticalLayout attrsWrap;
	private Grid<RemoteAttribute> attrsTable;
	private Span attrsTitleLabel;
	private VerticalLayout idsWrap;
	private Grid<RemoteIdentity> idsTable;
	private Span idsTitleLabel;
	private HorizontalLayout titleWrap;
	private Span noneLabel;
	private Html titleLabel;


	RemotelyAuthenticatedInputComponent(MessageSource msg)
	{
		this.msg = msg;
		buildMainLayout();
		add(mainLayout);
		setVisible(false);
		initLabels();
		initTables();
	}

	private void initLabels()
	{
		idsTitleLabel.setText(msg.getMessage("MappingResultComponent.idsTitle"));
		attrsTitleLabel.setText(msg.getMessage("MappingResultComponent.attrsTitle"));
		groupsTitleLabel.setText(msg.getMessage("MappingResultComponent.groupsTitle"));
		noneLabel.setText(msg.getMessage("MappingResultComponent.none"));
		groupsLabel.setText("");
	}

	private void initTables()
	{
		idsTable.addColumn(RemoteIdentity::getIdentityType)
				.setHeader(msg.getMessage("MappingResultComponent.idsTable.type"))
				.setAutoWidth(true);
		idsTable.addColumn(RemoteIdentity::getName).setHeader(
				msg.getMessage("MappingResultComponent.idsTable.value"))
				.setAutoWidth(true);

		attrsTable.addColumn(RemoteAttribute::getName).setHeader(
				msg.getMessage("MappingResultComponent.attrsTable.name"))
				.setAutoWidth(true);
		attrsTable.addColumn(RemoteAttribute::getValues).setHeader(
				msg.getMessage("MappingResultComponent.attrsTable.value"))
				.setAutoWidth(true);
	}

	void displayAuthnInput(RemotelyAuthenticatedInput input)
	{
		if (input == null
				|| (input.getIdentities().isEmpty()
				&& input.getAttributes().isEmpty()
				&& input.getGroups().isEmpty()))
		{
			displayItsTables(Collections.<RemoteIdentity>emptyList());
			displayAttrsTable(Collections.<RemoteAttribute>emptyList());
			displayGroups(Collections.<RemoteGroupMembership>emptyList());
			noneLabel.setVisible(true);
		} else
		{
			titleLabel.setHtmlContent("<div>" + msg.getMessage("DryRun.RemotelyAuthenticatedContextComponent.title",
					input.getIdpName()) + "</div>");
			displayItsTables(input.getIdentities().values());
			displayAttrsTable(input.getAttributes().values());
			displayGroups(input.getGroups().values());
			noneLabel.setVisible(false);
		}
		setVisible(true);
	}

	private void displayItsTables(Collection<RemoteIdentity> collection)
	{
		idsTable.setItems(Collections.emptyList());
		if (collection.isEmpty())
		{
			idsWrap.setVisible(false);
		} else
		{
			idsWrap.setVisible(true);
			idsTable.setItems(collection);
			idsTable.setAllRowsVisible(true);
		}
	}

	private void displayAttrsTable(Collection<RemoteAttribute> collection)
	{
		attrsTable.setItems(Collections.emptyList());
		if (collection.isEmpty())
		{
			attrsWrap.setVisible(false);
		} else
		{
			attrsWrap.setVisible(true);
			attrsTable.setItems(collection);
			attrsTable.setAllRowsVisible(true);
		}
	}

	private void displayGroups(Collection<RemoteGroupMembership> collection)
	{
		if (collection.isEmpty())
		{
			groupsWrap.setVisible(false);
		} else
		{
			groupsWrap.setVisible(true);
			groupsLabel.setText(collection.toString());
		}
	}

	private void buildMainLayout()
	{
		// common part: create layout
		mainLayout = new VerticalLayout();
		mainLayout.setSizeFull();
		mainLayout.setPadding(false);

		// top-level component properties
		setSizeFull();

		// titleWrap
		titleWrap = buildTitleWrap();
		mainLayout.add(titleWrap);

		// mappingResultWrap
		mappingResultWrap = buildMappingResultWrap();
		mainLayout.add(mappingResultWrap);

	}

	private HorizontalLayout buildTitleWrap()
	{
		// common part: create layout
		titleWrap = new HorizontalLayout();
		titleWrap.setPadding(false);
		//titleWrap.setSpacing(true);

		// titleLabel
		titleLabel = new Html("<div></div>");
		titleWrap.add(titleLabel);

		// noneLabel
		noneLabel = new Span();
		noneLabel.setWidthFull();
		noneLabel.setText("Label");
		titleWrap.add(noneLabel);

		return titleWrap;
	}

	private VerticalLayout buildMappingResultWrap()
	{
		// common part: create layout
		mappingResultWrap = new VerticalLayout();

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

	private VerticalLayout buildIdsWrap()
	{
		idsWrap = new VerticalLayout();
		idsWrap.setPadding(false);

		// idsTitleLabel
		idsTitleLabel = new Span();
		idsTitleLabel.setWidthFull();
		idsTitleLabel.setText("Label");
		idsWrap.add(idsTitleLabel);

		idsTable = new Grid<>();
		idsTable.setWidthFull();
		idsWrap.add(idsTable);

		return idsWrap;
	}

	private VerticalLayout buildAttrsWrap()
	{
		// common part: create layout
		attrsWrap = new VerticalLayout();
		attrsWrap.setPadding(false);

		// attrsTitleLabel
		attrsTitleLabel = new Span();
		attrsTitleLabel.setWidthFull();
		attrsTitleLabel.setText("Label");
		attrsWrap.add(attrsTitleLabel);

		// attrsTable
		attrsTable = new Grid<>();
		attrsTable.setWidthFull();
		attrsWrap.add(attrsTable);

		return attrsWrap;
	}

	private HorizontalLayout buildGroupsWrap()
	{
		// common part: create layout
		groupsWrap = new HorizontalLayout();
		groupsWrap.setPadding(false);
		//groupsWrap.setSpacing(true);

		// groupsTitleLabel
		groupsTitleLabel = new Span();
		groupsTitleLabel.setWidthFull();
		groupsTitleLabel.setText("Label");
		groupsWrap.add(groupsTitleLabel);

		// groupsLabel
		groupsLabel = new Span();
		groupsLabel.setWidthFull();
		groupsLabel.setText("Label");
		groupsWrap.add(groupsLabel);

		return groupsWrap;
	}

}
