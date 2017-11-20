/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributetype;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.v7.ui.ComboBox;
import com.vaadin.v7.ui.ProgressBar;
import com.vaadin.v7.ui.Upload;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webadmin.utils.FileUploder;
import pl.edu.icm.unity.webui.common.AbstractDialog;
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
	private ComboBox source;
	private ComboBox predefinedFiles;
	private AttributeTypeSupport attrTypeSupport;

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
		FormLayout main = new FormLayout();
		mode = new CheckBox(msg.getMessage("ImportAttributeTypes.overwrite"));

		source = new ComboBox(msg.getMessage("ImportAttributeTypes.source"));
		source.addItem(SourceType.File);
		source.setNullSelectionAllowed(false);

		predefinedResources = attrTypeSupport.getAttibuteTypeResourcesFromClasspathDir();
		
		predefinedFiles = new ComboBox(
				msg.getMessage("ImportAttributeTypes.source.predefinedSet"));

		if (!predefinedResources.isEmpty())
		{
			for (Resource f : predefinedResources)
			{
				predefinedFiles.addItem(FilenameUtils.getBaseName(f.getFile().getName()));
			}
			predefinedFiles.setNullSelectionAllowed(false);
			predefinedFiles.setValue(predefinedFiles.getItemIds().iterator().next());
			source.addItem(SourceType.PredefinedSet);
		}

		Label fileUploaded = new Label();
		ProgressBar progress = new ProgressBar();
		progress.setVisible(false);
		Upload upload = new Upload();
		uploader = new FileUploder(upload, progress, fileUploaded, msg, serverConfig
				.getFileValue(UnityServerConfiguration.WORKSPACE_DIRECTORY, true));
		uploader.register();

		source.addValueChangeListener((e) -> {
			if (source.getValue().equals(SourceType.File))
			{
				upload.setVisible(true);
				fileUploaded.setVisible(true);
				predefinedFiles.setVisible(false);
			} else
			{
				upload.setVisible(false);
				fileUploaded.setVisible(false);
				predefinedFiles.setVisible(true);
			}

		});

		source.setValue(SourceType.File);

		main.addComponents(mode, source, upload, fileUploaded, progress, predefinedFiles);

		return main;
	}
	
	@Override
	protected void onCancel()
	{
		uploader.clear();
		close();
	}

	private void loadAttributeTypesFromResource(Resource r)
	{
		List<AttributeType> toAdd = null;
		try
		{
			toAdd = attrTypeSupport.loadAttributeTypesFromResource(r);

		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("error"),
					msg.getMessage("ImportAttributeTypes.cannotParseFile"));
		}
		uploader.clear();

		try
		{
			mergeAttributeTypes(toAdd, mode.getValue());
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("ImportAttributeTypes.errorImport"), e);
		}

		callback.run();
		close();
	}

	@Override
	protected void onConfirm()
	{
		if (source.getValue().equals(SourceType.File))
		{
			File file =  uploader.getFile();
			if (file != null)
				loadAttributeTypesFromResource(new FileSystemResource(file));
		} else
		{
			for (Resource f : predefinedResources)
				if (FilenameUtils.getBaseName(f.getFilename())
						.equals(predefinedFiles.getValue()))
					loadAttributeTypesFromResource(f);
		}
	}

	
	private void mergeAttributeTypes(List<AttributeType> toMerge, boolean overwrite)
			throws EngineException
	{
		Set<String> exiting = attrTypeMan.getAttributeTypesAsMap().keySet();
		for (AttributeType at : toMerge)
		{
			if (!exiting.contains(at.getName()))
			{
				attrTypeMan.addAttributeType(at);
			} else if (overwrite)
			{
				attrTypeMan.updateAttributeType(at);
			}
		}
	}
}
