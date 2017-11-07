/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributetype;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Upload;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
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
	public static final String ATTRIBUTE_TYPES_CLASSPATH = "attributeTypes";

	private enum SourceType
	{
		File, PredefinedSet
	};

	private CheckBox mode;
	private FileUploder uploader;
	private UnityServerConfiguration serverConfig;
	private ApplicationContext appContext;
	private AttributeTypeManagement attrTypeMan;
	private Runnable callback;
	private List<File> predefinedSourceFiles;
	private ComboBox source;
	private ComboBox predefinedFiles;

	public ImportAttributeTypeDialog(UnityMessageSource msg, String caption,
			UnityServerConfiguration serverConfig, ApplicationContext appContext,
			AttributeTypeManagement attrTypeMan, Runnable callback)
	{
		super(msg, caption);
		this.serverConfig = serverConfig;
		this.callback = callback;
		this.attrTypeMan = attrTypeMan;
		this.appContext = appContext;
	}

	@Override
	protected Component getContents() throws Exception
	{
		FormLayout main = new FormLayout();
		mode = new CheckBox(msg.getMessage("ImportAttributeTypes.overwrite"));

		source = new ComboBox(msg.getMessage("ImportAttributeTypes.source"));
		source.addItem(SourceType.File);
		source.setNullSelectionAllowed(false);

		predefinedSourceFiles = loadFilesFromClasspathResource();
		predefinedFiles = new ComboBox(
				msg.getMessage("ImportAttributeTypes.source.predefinedSet"));

		if (!predefinedSourceFiles.isEmpty())
		{
			for (File f : predefinedSourceFiles)
			{
				predefinedFiles.addItem(FilenameUtils.getBaseName(f.getName()));
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

	private void loadAttributeTypesFromFile(File f)
	{
		List<AttributeType> toAdd = new ArrayList<>();
		if (f == null)
		{
			return;
		}

		JsonFactory jsonF = new JsonFactory(Constants.MAPPER);
		jsonF.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);

		try
		{
			BufferedInputStream is = new BufferedInputStream(new FileInputStream(f));
			JsonParser jp = jsonF.createParser(is);
			if (jp.nextToken() == JsonToken.START_ARRAY)
				jp.nextToken();

			while (jp.currentToken() == JsonToken.START_OBJECT)
			{
				AttributeType at = new AttributeType(jp.readValueAsTree());
				toAdd.add(at);
				jp.nextToken();
			}

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
			loadAttributeTypesFromFile(uploader.getFile());
		} else
		{
			for (File f : predefinedSourceFiles)
				if (FilenameUtils.getBaseName(f.getName())
						.equals(predefinedFiles.getValue()))
					loadAttributeTypesFromFile(f);
		}
	}

	private Set<String> getExitingAttributeTypes() throws EngineException
	{

		return attrTypeMan.getAttributeTypesAsMap().keySet();

	}

	private List<File> loadFilesFromClasspathResource()
	{

		ArrayList<File> files = new ArrayList<>();
		Resource[] resources = null;
		try
		{
			resources = appContext.getResources(
					"classpath:" + ATTRIBUTE_TYPES_CLASSPATH + "/*.json");
		} catch (Exception e)
		{
			return files;
		}

		if (resources == null || resources.length == 0)
		{
			return files;
		}
		try
		{
			for (Resource r : resources)
			{

				files.add(r.getFile());

			}
		} catch (IOException e)
		{
			throw new InternalException(
					"Can't load attribute type json files from classpath: "
							+ ATTRIBUTE_TYPES_CLASSPATH,
					e);
		}
		return files;

	}

	private void mergeAttributeTypes(List<AttributeType> toMerge, boolean overwrite)
			throws EngineException
	{
		Set<String> exiting = getExitingAttributeTypes();
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
