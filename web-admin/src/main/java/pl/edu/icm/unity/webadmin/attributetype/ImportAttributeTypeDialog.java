/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributetype;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
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
	public enum ImportMode
	{
		MERGE, OVERWRITE
	};
 
	private ComboBox mode;
	private FileUploder uploader;
	private final UnityServerConfiguration serverConfig;
	private AttributeTypeManagement attrTypeMan;
	private Runnable callback;
	
	
	public ImportAttributeTypeDialog(UnityMessageSource msg, String caption,
			UnityServerConfiguration serverConfig, AttributeTypeManagement attrTypeMan,
			Runnable callback)
	{
		super(msg, caption);
		this.serverConfig = serverConfig;
		this.callback = callback;
		this.attrTypeMan = attrTypeMan;
	}

	

	@Override
	protected Component getContents() throws Exception
	{
		FormLayout vl = new FormLayout();
		mode = new ComboBox(msg.getMessageNullArg("ImportAttributeTypes.mode"));
		for (ImportMode im : ImportMode.values())
			mode.addItem(im.toString());		
		mode.setNullSelectionAllowed(false);
		mode.select(ImportMode.values()[0].toString());
			
		Label fileUploaded = new Label();
		ProgressBar progress = new ProgressBar();
		progress.setVisible(false);
		Upload upload = new Upload();
		uploader = new FileUploder(upload, progress, fileUploaded, msg, serverConfig.getFileValue(
				UnityServerConfiguration.WORKSPACE_DIRECTORY, true));
		uploader.register();	
		vl.addComponents(mode, upload,fileUploaded, progress);	
		return vl;
	}

	@Override
	protected void onCancel()
	{
		uploader.clear();
		close();
	}
	
	@Override
	protected void onConfirm()
	{
	
		List<AttributeType> toAdd = new ArrayList<>();
		
		final File f = uploader.getFile();
		
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
				
			while(jp.currentToken() == JsonToken.START_OBJECT)
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
			mergeAttributeTypes(toAdd, mode.getValue().toString().equals(ImportMode.OVERWRITE.toString()));
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("ImportAttributeTypes.errorImport"),
					e);
		}
			
		callback.run();
		close();	
	}
	
	private Set<String> getExitingAttributeTypes() throws EngineException
	{
	
		return attrTypeMan.getAttributeTypesAsMap().keySet();
		
	}

	private void mergeAttributeTypes(List<AttributeType> toMerge, boolean overwrite) throws EngineException
	{
		Set<String> exiting = getExitingAttributeTypes();
		for (AttributeType at : toMerge)
		{
			if (!exiting.contains(at.getName()))
			{
				attrTypeMan.addAttributeType(at);
			}
			else if (overwrite)
			{
				attrTypeMan.updateAttributeType(at);
			}
		}	
	}
}
