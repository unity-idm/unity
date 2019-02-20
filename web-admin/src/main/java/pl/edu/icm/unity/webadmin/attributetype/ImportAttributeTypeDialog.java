/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributetype;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webadmin.utils.FileUploder;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Allows import attribute types from json file
 * 
 * @author P.Piernik
 *
 */
public class ImportAttributeTypeDialog extends AbstractDialog
{
	private enum SourceType
	{
		File, PredefinedSet
	};

	private CheckBox mode;
	private FileUploder uploader;
	private UnityServerConfiguration serverConfig;
	private AttributeTypeManagement attrTypeMan;
	private Runnable callback;
	private List<Resource> predefinedResources;
	private ComboBox<SourceType> source;
	private ComboBox<String> predefinedFiles;
	private AttributeTypeSupport attrTypeSupport;
	private GenericElementsTable<AttributeType> selectionTable;
	private CheckBox filterExisting;

	public ImportAttributeTypeDialog(UnityMessageSource msg, String caption,
			UnityServerConfiguration serverConfig,
			AttributeTypeManagement attrTypeMan,
			AttributeTypeSupport attrTypeSupport, Runnable callback)
	{
		super(msg, caption);
		this.serverConfig = serverConfig;
		this.callback = callback;
		this.attrTypeMan = attrTypeMan;
		this.attrTypeSupport = attrTypeSupport;
	}

	@Override
	protected Component getContents() throws Exception
	{
		FormLayout mainSelection = new FormLayout();
		mode = new CheckBox(msg.getMessage("ImportAttributeTypes.overwrite"));

		source = new ComboBox<>(msg.getMessage("ImportAttributeTypes.source"));
		List<SourceType> sources = new ArrayList<>();
		source.setDataProvider(new ListDataProvider<>(sources));
		sources.add(SourceType.File);
		source.setEmptySelectionAllowed(false);

		predefinedResources = attrTypeSupport.getAttibuteTypeResourcesFromClasspathDir();
		
		predefinedFiles = new ComboBox<>(
				msg.getMessage("ImportAttributeTypes.source.predefinedSet"));

		if (!predefinedResources.isEmpty())
		{
			List<String> predefined = new ArrayList<>();
			for (Resource resource: predefinedResources)
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
		uploader = new FileUploder(upload, progress, fileUploaded, msg, serverConfig
				.getFileValue(UnityServerConfiguration.WORKSPACE_DIRECTORY, true), 
				() -> { reloadTableFromFile();} );
		uploader.register();
		
		selectionTable = new GenericElementsTable<>(msg.getMessage("ImportAttributeTypes.typesToImport"), 
				element -> element.getName());
		selectionTable.setSizeFull();
		selectionTable.setMultiSelect(true);
		
		filterExisting = new CheckBox(msg.getMessage("ImportAttributeTypes.filterExisting"), false);
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
		
		return main;
	}

	private void updateFilter(boolean add)
	{
		if (!add)
		{
			selectionTable.clearFilters();
			return;
		}
		final Set<String> existing = new HashSet<>();
		try
		{
			existing.addAll(attrTypeMan.getAttributeTypesAsMap().keySet());
		} catch (Exception e)
		{

		}
		selectionTable.addFilter(a -> !existing.contains(a.getName()));
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
		setSelectionTableVisiable(!selectionTable.getElements().isEmpty());
	}

	private void reloadTableFromFile()
	{
		filterExisting.setValue(false);
		File file = uploader.getFile();
		if (file != null)
		{
			loadAttributeTypesFromResource(
					new FileSystemResource(file));
		}
		uploader.unblock();
		setSelectionTableVisiable(!selectionTable.getElements().isEmpty());
	}
	
	private  void setSelectionTableVisiable(boolean visible)
	{
		filterExisting.setVisible(visible);
		selectionTable.setVisible(visible);	
	}
	
	@Override
	protected void onCancel()
	{
		uploader.clear();
		close();
	}

	private void loadAttributeTypesFromResource(Resource r)
	{
		Set<AttributeType> toAdd = new HashSet<>();
		try
		{
			toAdd.addAll(attrTypeSupport.loadAttributeTypesFromResource(r));

		} catch (Exception e)
		{
			NotificationPopup.showError(
					msg.getMessage("ImportAttributeTypes.cannotParseFile"),
					e.getCause() != null ? e.getCause().getMessage()
							: e.toString());
					
		}
		
		selectionTable.setInput(toAdd);
		toAdd.forEach(selectionTable::select);	
	}

	@Override
	protected void onConfirm()
	{	
		mergeAttributeTypes(selectionTable.getSelectedItems(), mode.getValue());
		uploader.clear();
		callback.run();
		close();
	}

	private void mergeAttributeTypes(Set<AttributeType> toMerge, boolean overwrite)
	{

		try
		{
			Set<String> existing = attrTypeMan.getAttributeTypesAsMap().keySet();
			for (AttributeType at : toMerge)
			{

				if (!existing.contains(at.getName()))
				{
					attrTypeMan.addAttributeType(at);
				} else if (overwrite)
				{
					attrTypeMan.updateAttributeType(at);
				}
			}
		} catch (Exception e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("ImportAttributeTypes.errorImport"), e);
		}
	}
}
