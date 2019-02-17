/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webadmin.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.SucceededEvent;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AbstractUploadReceiver;
import pl.edu.icm.unity.webui.common.LimitedOuputStream;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * Allows for uploading file. Show error popup if problem occurs.
 * @author P.Piernik
 *
 */
public class FileUploder extends AbstractUploadReceiver
{
	private static final int MAX_SIZE = 50000000;
	private Label info;
	private File target;
	private LimitedOuputStream fos;
	private boolean uploading = false;
	private boolean blocked = false;
	private UnityMessageSource msg;
	private File tempDir;
	private Runnable uploadSucceededCallback;

	public FileUploder(Upload upload, ProgressBar progress, Label info, UnityMessageSource msg, File tempDir, Runnable callback)
	{
		super(upload, progress);
		this.info = info;
		this.msg = msg;
		this.tempDir = tempDir;
		this.uploadSucceededCallback = callback;
		info.setValue(msg.getMessage("FileUploader.noFileUploaded"));
		upload.setCaption(msg.getMessage("FileUploader.uploadCaption"));
	}
	
	public FileUploder(Upload upload, ProgressBar progress, Label info, UnityMessageSource msg, File tempDir)
	{
		this(upload, progress, info, msg, tempDir, null);
	}
	
	@Override
	public synchronized void uploadSucceeded(SucceededEvent event)
	{
		super.uploadSucceeded(event);
		if (!fos.isOverflow() && fos.getLength() > 0)
			info.setValue(msg.getMessage("FileUploader.fileUploaded",
					new Date()));
		else if (fos.getLength() == 0)
			info.setValue(msg.getMessage("FileUploader.uploadFileEmpty"));
		else
			info.setValue(msg.getMessage("FileUploader.uploadFileTooBig"));
		setUploading(false);
		if (uploadSucceededCallback != null)
			uploadSucceededCallback.run();
	}

	@Override
	public synchronized OutputStream receiveUpload(String filename, String mimeType)
	{
		if (blocked || uploading)
			throw new IllegalStateException(
					"Upload is in progress");
		try
		{
			if (target != null && target.exists())
				target.delete();
			target = createImportFile();
			setUploading(true);
			fos = new LimitedOuputStream(MAX_SIZE, new BufferedOutputStream(
					new FileOutputStream(target)));
			return fos;
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private synchronized void setUploading(boolean how)
	{
		this.uploading = how;
	}

	public synchronized boolean isUploading()
	{
		return uploading;
	}

	public synchronized File getFile()
	{
		if (isOverflow())
		{
			NotificationPopup.showError(msg.getMessage("error"),
					msg.getMessage("FileUploader.uploadFileTooBig"));
			return null;
		}
		if (isUploading())
		{
			NotificationPopup.showError(msg.getMessage("error"),
					msg.getMessage("FileUploader.uploadInProgress"));
			return null;
		}	
		
		if (target == null)
		{
			NotificationPopup.showError(msg.getMessage("error"),
					msg.getMessage("FileUploader.uploadFileFirst"));
			return null;
		}
		blocked = true;
		return target;
	}

	public synchronized void unblock()
	{
		blocked = false;
	}

	public synchronized boolean isOverflow()
	{
		if (fos == null)
			return false;
		return fos.isOverflow();
	}
	
	private File createImportFile() throws IOException
	{
		File workspace = tempDir;
		if (!workspace.exists())
			workspace.mkdir();
		File ret  = File.createTempFile("temp","", workspace);
		ret.deleteOnExit();
		if (ret.exists())
			ret.delete();
		return ret;
	}
	
	public synchronized void clear()
	{
		info.setValue(msg.getMessage("FileUploader.noFileUploaded"));
		if (target != null && target.exists())
			target.delete();
		uploading = false;
		blocked = false;
	}
};