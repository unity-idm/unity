/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directorySetup.attributeTypes;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.FileUploder;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Allows import attribute types from json file
 * 
 * @author P.Piernik
 *
 */
class ImportAttributeTypeEditor extends CustomComponent
{
	private enum SourceType
	{
		File, PredefinedSet
	};

	private MessageSource msg;

	private CheckBox mode;
	private FileUploder uploader;
	private UnityServerConfiguration serverConfig;
	private List<Resource> predefinedResources;
	private ComboBox<SourceType> source;
	private ComboBox<String> predefinedFiles;
	private AttributeTypeSupport attrTypeSupport;
	private GenericElementsTable<AttributeType> selectionTable;
	private CheckBox filterExisting;

	private Collection<AttributeType> existing;

	ImportAttributeTypeEditor(MessageSource msg, Collection<AttributeType> existing,
			UnityServerConfiguration serverConfig, AttributeTypeSupport attrTypeSupport) throws Exception
	{
		this.msg = msg;
		this.serverConfig = serverConfig;
		this.attrTypeSupport = attrTypeSupport;
		this.existing = existing;
		initUI();
	}

	private void initUI() throws Exception
	{
		FormLayout mainSelection = new FormLayout();
		mode = new CheckBox(msg.getMessage("ImportAttributeTypeEditor.overwrite"));

		source = new ComboBox<>(msg.getMessage("ImportAttributeTypeEditor.source"));
		List<SourceType> sources = new ArrayList<>();
		source.setDataProvider(new ListDataProvider<>(sources));
		sources.add(SourceType.File);
		source.setEmptySelectionAllowed(false);

		predefinedResources = attrTypeSupport.getAttibuteTypeResourcesFromClasspathDir();

		predefinedFiles = new ComboBox<>(msg.getMessage("ImportAttributeTypeEditor.source.predefinedSet"));

		if (!predefinedResources.isEmpty())
		{
			List<String> predefined = new ArrayList<>();
			for (Resource resource : predefinedResources)
			{
				String name = FilenameUtils.getBaseName(resource.getFilename());
				predefined.add(name);
			}

			predefinedFiles.setItems(predefined);
			predefinedFiles.setEmptySelectionAllowed(false);
			predefinedFiles.setSelectedItem(predefined.get(0));
			sources.add(SourceType.PredefinedSet);
		}

		Label fileUploaded = new Label();
		ProgressBar progress = new ProgressBar();
		progress.setVisible(false);
		Upload upload = new Upload();
		uploader = new FileUploder(upload, progress, fileUploaded, msg,
				serverConfig.getFileValue(UnityServerConfiguration.WORKSPACE_DIRECTORY, true), () -> {
					reloadTableFromFile();
				});
		uploader.register();

		selectionTable = new GenericElementsTable<>(msg.getMessage("ImportAttributeTypeEditor.typesToImport"),
				element -> element.getName());
		selectionTable.setSizeFull();
		selectionTable.setMultiSelect(true);

		filterExisting = new CheckBox(msg.getMessage("ImportAttributeTypeEditor.filterExisting"), false);
		filterExisting.addValueChangeListener(e -> updateFilter(e.getValue()));

		source.addValueChangeListener((e) -> {
			setSelectionTableVisiable(false);
			filterExisting.setValue(false);

			if (source.getValue().equals(SourceType.File))
			{
				upload.setVisible(true);
				fileUploaded.setVisible(true);
				predefinedFiles.setVisible(false);
				selectionTable.setInput(Collections.emptyList());
			} else
			{
				uploader.clear();
				upload.setVisible(false);
				fileUploaded.setVisible(false);
				predefinedFiles.setVisible(true);
				selectionTable.setInput(Collections.emptyList());
				reloadTableFromPredefinedSet();

			}
		});

		predefinedFiles.addValueChangeListener((e) -> {
			reloadTableFromPredefinedSet();
		});

		source.setValue(SourceType.File);

		mainSelection.addComponents(mode, source, upload, fileUploaded, progress, predefinedFiles);
		VerticalLayout main = new VerticalLayout(mainSelection, filterExisting, selectionTable);
		main.setMargin(false);

		setCompositionRoot(main);
	}

	private void updateFilter(boolean add)
	{
		if (!add)
		{
			selectionTable.clearFilters();
			return;
		}

		selectionTable.addFilter(a -> !existing.stream().map(at -> at.getName()).collect(Collectors.toSet())
				.contains(a.getName()));
	}

	private void reloadTableFromPredefinedSet()
	{
		filterExisting.setValue(false);
		for (Resource f : predefinedResources)
			if (FilenameUtils.getBaseName(f.getFilename()).equals(predefinedFiles.getValue()))
			{
				loadAttributeTypesFromResource(f);
			}
		setSelectionTableVisiable(!selectionTable.getElements().isEmpty());
	}

	private void reloadTableFromFile()
	{
		filterExisting.setValue(false);
		File file = uploader.getFile();
		if (file != null)
		{
			loadAttributeTypesFromResource(new FileSystemResource(file));
		}
		uploader.unblock();
		setSelectionTableVisiable(!selectionTable.getElements().isEmpty());
	}

	private void setSelectionTableVisiable(boolean visible)
	{
		filterExisting.setVisible(visible);
		selectionTable.setVisible(visible);
	}

	private void loadAttributeTypesFromResource(Resource r)
	{
		Set<AttributeType> toAdd = new HashSet<>();
		try
		{
			toAdd.addAll(attrTypeSupport.loadAttributeTypesFromResource(r));

		} catch (Exception e)
		{
			NotificationPopup.showError(msg.getMessage("ImportAttributeTypeEditor.cannotParseFile"),
					e.getCause() != null ? e.getCause().getMessage() : e.toString());

		}

		selectionTable.setInput(toAdd);
		toAdd.forEach(selectionTable::select);
	}

	Set<AttributeType> getAttributeTypes()
	{
		uploader.clear();
		return selectionTable.getSelectedItems();
	}

	boolean isOverwriteMode()
	{
		return mode.getValue();
	}
	
	void clear()
	{
		uploader.clear();
	}
}
