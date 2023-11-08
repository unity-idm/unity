/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_setup.attribute_types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.GenericElementsTable;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;

/**
 * Allows import attribute types from json file
 * 
 * @author P.Piernik
 *
 */
class ImportAttributeTypeEditor extends VerticalLayout
{
	private static final int MAX_FILE_SIZE_IN_BYTES = 50000000;
	
	private enum SourceType
	{
		File, PredefinedSet
	};

	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private final AttributeTypeSupport attrTypeSupport;

	private Checkbox mode;
	private List<Resource> predefinedResources;
	private ComboBox<SourceType> source;
	private ComboBox<String> predefinedFiles;
	private GenericElementsTable<AttributeType> selectionTable;
	private Checkbox filterExisting;
	private MemoryBuffer memoryBuffer;
	private Collection<AttributeType> existing;
	private Upload upload;

	ImportAttributeTypeEditor(MessageSource msg, Collection<AttributeType> existing,
			 AttributeTypeSupport attrTypeSupport,
			NotificationPresenter notificationPresenter) throws Exception
	{
		this.msg = msg;
		this.attrTypeSupport = attrTypeSupport;
		this.existing = existing;
		this.notificationPresenter = notificationPresenter;
		initUI();
	}

	private void initUI() throws Exception
	{
		FormLayout mainSelection = new FormLayout();
		mainSelection.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		mainSelection.setWidthFull();
		
		FormLayout predefinedSelection = new FormLayout();
		predefinedSelection.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		predefinedSelection.setWidthFull();
		
		FormLayout uploadLayout = new FormLayout();
		uploadLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		uploadLayout.setWidthFull();
		
		mode = new Checkbox();
		source = new ComboBox<>();
		List<SourceType> sources = new ArrayList<>();
		sources.add(SourceType.File);
		predefinedResources = attrTypeSupport.getAttibuteTypeResourcesFromClasspathDir();

		predefinedFiles = new ComboBox<>();

		if (!predefinedResources.isEmpty())
		{
			List<String> predefined = new ArrayList<>();
			for (Resource resource : predefinedResources)
			{
				String name = FilenameUtils.getBaseName(resource.getFilename());
				predefined.add(name);
			}

			predefinedFiles.setItems(predefined);
			predefinedFiles.setValue(predefined.get(0));
			sources.add(SourceType.PredefinedSet);
		}
		source.setItems(sources);

		NativeLabel fileUploaded = new NativeLabel();
		ProgressBar progress = new ProgressBar();
		progress.setVisible(false);

		memoryBuffer = new MemoryBuffer();
		upload = new Upload(memoryBuffer);
		upload.setMaxFileSize(MAX_FILE_SIZE_IN_BYTES);
		upload.setAcceptedFileTypes("application/json");
		upload.addFinishedListener(e -> reloadTableFromFile());
		upload.getElement()
				.addEventListener("file-remove", e -> clear());
		upload.addFileRejectedListener(
				e -> notificationPresenter.showError(msg.getMessage("error"), e.getErrorMessage()));

		upload.setDropAllowed(false);
		upload.setWidth("21em");

		selectionTable = new GenericElementsTable<>(msg.getMessage("ImportAttributeTypeEditor.typesToImport"),
				element -> element.getName());

		selectionTable.setMultiSelect(true);
		selectionTable.setMaxHeight(30, Unit.EM);

		filterExisting = new Checkbox(msg.getMessage("ImportAttributeTypeEditor.filterExisting"), false);
		filterExisting.addValueChangeListener(e -> updateFilter(e.getValue()));

		source.addValueChangeListener((e) ->
		{
			setSelectionTableVisiable(false);
			filterExisting.setValue(false);

			if (source.getValue()
					.equals(SourceType.File))
			{
				upload.setVisible(true);
				uploadLayout.setVisible(true);
				fileUploaded.setVisible(true);
				predefinedFiles.setVisible(false);
				predefinedSelection.setVisible(false);
				selectionTable.setInput(Collections.emptyList());
			} else
			{
				upload.setVisible(false);
				uploadLayout.setVisible(false);
				fileUploaded.setVisible(false);
				predefinedFiles.setVisible(true);
				predefinedSelection.setVisible(true);
				selectionTable.setInput(Collections.emptyList());
				reloadTableFromPredefinedSet();
			}
		});

		predefinedFiles.addValueChangeListener((e) ->
		{
			reloadTableFromPredefinedSet();
		});

		source.setValue(SourceType.File);

		mainSelection.addFormItem(mode, msg.getMessage("ImportAttributeTypeEditor.overwrite"));
		mainSelection.addFormItem(source, msg.getMessage("ImportAttributeTypeEditor.source"));
		uploadLayout.addFormItem(upload, "");
		predefinedSelection.addFormItem(predefinedFiles, msg.getMessage("ImportAttributeTypeEditor.source.predefinedSet"));
		removeAll();
		add(mainSelection, predefinedSelection, uploadLayout,  filterExisting, selectionTable);
	}

	private void updateFilter(boolean add)
	{
		if (!add)
		{
			selectionTable.clearFilters();
			return;
		}

		selectionTable.addFilter(a -> !existing.stream()
				.map(at -> at.getName())
				.collect(Collectors.toSet())
				.contains(a.getName()));
	}

	private void reloadTableFromPredefinedSet()
	{
		filterExisting.setValue(false);
		for (Resource f : predefinedResources)
			if (FilenameUtils.getBaseName(f.getFilename())
					.equals(predefinedFiles.getValue()))
			{
				loadAttributeTypesFromResource(f);
			}
		setSelectionTableVisiable(!selectionTable.getElements()
				.isEmpty());
	}

	private void reloadTableFromFile()
	{
		filterExisting.setValue(false);
		loadAttributeTypesFromResource(new InputStreamResource(memoryBuffer.getInputStream()));
		setSelectionTableVisiable(!selectionTable.getElements()
				.isEmpty());
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
			notificationPresenter.showError(msg.getMessage("ImportAttributeTypeEditor.cannotParseFile"),
					e.getMessage());
		}

		selectionTable.setInput(toAdd);
		toAdd.forEach(selectionTable::select);
	}

	Set<AttributeType> getAttributeTypes()
	{
		return selectionTable.getSelectedItems();
	}

	boolean isOverwriteMode()
	{
		return mode.getValue();
	}

	void clear()
	{
		selectionTable.clear();
		upload.clearFileList();
		selectionTable.setVisible(false);
	}
}
