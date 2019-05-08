/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.file;

import org.vaadin.simplefiledownloader.SimpleFileDownloader;

import com.vaadin.server.StreamResource;
import com.vaadin.ui.Button;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.FileStreamResource;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;

/**
 * Allows upload file or set remote file url
 * @author P.Piernik
 *
 */
public class FileField extends FileFieldBase
{
	private SimpleFileDownloader downloader;
	private Button downloadButton;
	private String fileName;
	
	public FileField(UnityMessageSource msg, String mimeType, String previewFileName)
	{
		super(msg, mimeType);
		this.fileName = previewFileName;
		downloader = new SimpleFileDownloader();
		addExtension(downloader);
		downloadButton = new Button();
		downloadButton.setDescription(msg.getMessage("FileField.download"));
		downloadButton.setIcon(Images.download.getResource());
		downloadButton.addClickListener(e -> downloader.download());
		downloadButton.setVisible(false);
		
		tab.addSelectedTabChangeListener(e -> {
			if (tab.getSelectedTab().equals(remoteTab.getComponent()))
			{
				downloadButton.setVisible(false);
			}else
			{
				if (getValue()!= null && getValue().getLocal()!=null)
				{
					downloadButton.setVisible(true);
				}
			}
		});
	
		main.addComponent(downloadButton);	
	}
	
	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		downloadButton.setEnabled(enabled);
	}
	
	@Override
	protected void setPreview()
	{
		LocalOrRemoteResource value = getValue();
		downloadButton.setVisible(false);
		downloader.setFileDownloadResource(null);
	
		if (value.getLocal() != null)
		{
			downloader.setFileDownloadResource(new StreamResource(new FileStreamResource(value.getLocal()), fileName));
			downloadButton.setVisible(true);
		} 
	}	
}
