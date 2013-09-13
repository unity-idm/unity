/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.serverman;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.ServerManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.webui.common.AbstractUploadReceiver;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.LimitedOuputStream;
import pl.edu.icm.unity.webui.common.ConfirmDialog.Callback;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.server.FileResource;
import com.vaadin.server.Page;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Panel;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Upload.SucceededEvent;

/**
 * Responsible for exporting and importing the server's database.
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ImportExportComponent extends VerticalLayout
{
	private static final int MAX_SIZE = 10000000;
	private static final long DUMP_STORE_TIME = 3600000;
	private final UnityMessageSource msg;
	private final ServerManagement serverManagement;
	private final UnityServerConfiguration serverConfig;
	private VerticalLayout downloadLinks;
	private Label infoNoDumps;
	private DumpUploader uploader;
	
	@Autowired
	public ImportExportComponent(UnityMessageSource msg, ServerManagement serverManagement,
			UnityServerConfiguration serverConfig)
	{
		this.msg = msg;
		this.serverManagement = serverManagement;
		this.serverConfig = serverConfig;
		initUI();
	}

	private void initUI()
	{
		setCaption(msg.getMessage("ImportExport.caption"));
		setMargin(true);
		setSpacing(true);
		initExportUI();
		initImportUI();
	}
	
	private void initExportUI()
	{
		Panel exportPanel = new Panel(msg.getMessage("ImportExport.exportCaption"));
		VerticalLayout hl = new VerticalLayout();
		hl.setSpacing(true);
		hl.setMargin(true);
		exportPanel.setContent(hl);
		
		Button createDump = new Button(msg.getMessage("ImportExport.createDump"));
		createDump.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				createDump();
			}
		});
		Label info = new Label(msg.getMessage("ImportExport.dumps"));
		infoNoDumps = new Label(msg.getMessage("ImportExport.noDumps"));
		Label info2 = new Label(msg.getMessage("ImportExport.dumpsExpiry", DUMP_STORE_TIME/60000));
		info2.addStyleName(Styles.italic.toString());
		downloadLinks = new VerticalLayout();
		downloadLinks.setSpacing(true);
		Button removeAll = new Button(msg.getMessage("ImportExport.removeAll"));
		removeAll.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				removeAllDumps();
			}
		});
		hl.addComponents(createDump, info, downloadLinks, infoNoDumps, info2, removeAll);
		
		refreshDumps();
		
		addComponent(exportPanel);
	}
	
	private void initImportUI()
	{
		Panel importPanel = new Panel(msg.getMessage("ImportExport.importCaption"));
		VerticalLayout vl = new VerticalLayout();
		vl.setSpacing(true);
		vl.setMargin(true);
		importPanel.setContent(vl);
		
		Label info = new Label(msg.getMessage("ImportExport.uploadInfo"));
		Label fileUploaded = new Label(msg.getMessage("ImportExport.noFileUploaded"));
		
		ProgressIndicator progress = new ProgressIndicator();
		progress.setVisible(false);
		Upload upload = new Upload();
		upload.setCaption(msg.getMessage("ImportExport.uploadCaption"));
		uploader = new DumpUploader(upload, progress, fileUploaded);
		upload.setReceiver(uploader);
		upload.addSucceededListener(uploader);
		upload.addStartedListener(uploader);
		upload.addProgressListener(uploader);
		
		Button importDump = new Button(msg.getMessage("ImportExport.import"));
		importDump.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				importDumpInit();
			}
		});
		
		vl.addComponents(info, upload, progress, fileUploaded, importDump);
		addComponent(importPanel);
	}
	
	private void importDumpInit()
	{
		if (uploader.isOverflow())
		{
			ErrorPopup.showError(msg.getMessage("error"), msg.getMessage("ImportExport.uploadFileTooBig"));
			return;
		}
		if (uploader.isUploading())
		{
			ErrorPopup.showError(msg.getMessage("error"), msg.getMessage("ImportExport.uploadInProgress"));
			return;
		}
			
		final File f = uploader.getFile();
		if (f == null)
		{
			ErrorPopup.showError(msg.getMessage("error"), msg.getMessage("ImportExport.uploadFileFirst"));
			return;
		}
		
		ConfirmDialog confirm = new ConfirmDialog(msg, msg.getMessage("ImportExport.confirm"), 
				new Callback()
		{
			@Override
			public void onConfirm()
			{
				importDump(f);
			}
		});
		confirm.setHTMLContent(true);	
		confirm.show();
	}
	
	private void importDump(File from)
	{
		try
		{
			Page.getCurrent().getJavaScript().execute("window.location.reload();");
			serverManagement.importDb(from, true);
		} catch (Exception e)
		{
			uploader.unblock();
			ErrorPopup.showError(msg.getMessage("ImportExport.importFailed"), e);
		}
	}
	
	private void createDump()
	{
		int current = refreshDumps();
		if (current > 4)
		{
			ErrorPopup.showError(msg.getMessage("error"), 
					msg.getMessage("ImportExport.tooManyDumps"));
			return;
		}
		try
		{
			serverManagement.exportDb();
			ErrorPopup.showNotice(msg.getMessage("notice"), msg.getMessage("ImportExport.exportSucceeded"));
		} catch (EngineException e)
		{
			ErrorPopup.showError(msg.getMessage("ImportExport.exportFailed"), e);
		}
		refreshDumps();
	}
	
	private void removeAllDumps()
	{
		downloadLinks.removeAllComponents();
		
		File workspace = serverConfig.getFileValue(UnityServerConfiguration.WORKSPACE_DIRECTORY, true);
		File exportsDirectory = new File(workspace, ServerManagement.DB_DUMP_DIRECTORY);
		if (!exportsDirectory.exists())
			return;
		File[] files = exportsDirectory.listFiles();
		for (File file: files)
			file.delete();
		infoNoDumps.setVisible(true);
	}
	
	private int refreshDumps()
	{
		downloadLinks.removeAllComponents();
		
		File workspace = serverConfig.getFileValue(UnityServerConfiguration.WORKSPACE_DIRECTORY, true);
		File exportsDirectory = new File(workspace, ServerManagement.DB_DUMP_DIRECTORY);
		if (!exportsDirectory.exists())
			return 0;
		File[] files = exportsDirectory.listFiles();
		long now = System.currentTimeMillis();
		
		int ret = 0;
		for (File file: files)
		{
			if (file.lastModified() + DUMP_STORE_TIME < now)
			{
				file.delete();
				continue;
			}
			FileResource resource = new FileResource(file);
			String info = msg.getMessage("ImportExport.dumpCreatedAt", new Date(file.lastModified()));
			Link downloadLink = new Link(info, resource);
			downloadLinks.addComponent(downloadLink);
			ret++;
		}
		infoNoDumps.setVisible(ret == 0);
		return ret;
	}
	
	
	private class DumpUploader extends AbstractUploadReceiver
	{
		private Label info;
		private File target;
		private LimitedOuputStream fos;
		private boolean uploading = false;
		private boolean blocked = false;
		
		public DumpUploader(Upload upload, ProgressIndicator progress, Label info)
		{
			super(upload, progress);
			this.info = info;
		}

		@Override
		public void uploadSucceeded(SucceededEvent event) 
		{
			super.uploadSucceeded(event);
			if (!fos.isOverflow())
				info.setValue(msg.getMessage("ImportExport.dumpUploaded", new Date()));
			else
				info.setValue(msg.getMessage("ImportExport.uploadFileTooBig"));
			setUploading(false);
		}
		
		@Override
		public synchronized OutputStream receiveUpload(String filename, String mimeType) 
		{
			if (blocked || uploading)
				throw new IllegalStateException("Can't upload when update is in progress");
			try
			{
				if (target != null && target.exists())
					target.delete();
				target = createImportFile();
				setUploading(true);
				fos = new LimitedOuputStream(MAX_SIZE, 
						new BufferedOutputStream(new FileOutputStream(target)));
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
			if (target == null)
				return null;
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
	};
	
	private File createImportFile() throws IOException
	{
		File workspace = serverConfig.getFileValue(UnityServerConfiguration.WORKSPACE_DIRECTORY, true);
		File importDir = new File(workspace, ServerManagement.DB_IMPORT_DIRECTORY);
		if (!importDir.exists())
			importDir.mkdir();
		File ret = new File(importDir, "databaseDump-uploaded.json");
		if (ret.exists())
			ret.delete();
		return ret;
	}
}

