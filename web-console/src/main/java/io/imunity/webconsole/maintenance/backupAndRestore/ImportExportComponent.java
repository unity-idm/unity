/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.maintenance.backupAndRestore;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.data.Binder;
import com.vaadin.server.DownloadStream;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FileResource;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.types.basic.DBDumpContentElements;
import pl.edu.icm.unity.webui.common.AbstractUploadReceiver;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ConfirmDialog.Callback;
import pl.edu.icm.unity.webui.common.LimitedOuputStream;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Responsible for exporting and importing the server's database.
 * 
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ImportExportComponent extends VerticalLayout
{
	private final int MAX_OF_FREE_MEMORY_USAGE_IN_PERCENT = 70;
	
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, ImportExportComponent.class);

	
	private final MessageSource msg;
	private final ServerManagement serverManagement;
	private final UnityServerConfiguration serverConfig;
	private DumpUploader uploader;

	@Autowired
	ImportExportComponent(MessageSource msg, ServerManagement serverManagement,
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
		setMargin(false);
		setSpacing(true);
		initExportUI();
		initImportUI();
	}

	private void initExportUI()
	{
		Panel exportPanel = new SafePanel(msg.getMessage("ImportExport.exportCaption"));
		VerticalLayout hl = new VerticalLayout();
		hl.setSpacing(true);
		hl.setMargin(true);
		exportPanel.setContent(hl);

		Binder<BinderDBDumpContentElements> configBinder = new Binder<>(BinderDBDumpContentElements.class);
		
		CheckBox systemConfig = new CheckBox(msg.getMessage("ImportExport.systemConfig"));
		configBinder.forField(systemConfig).bind("systemConfig");
		
		CheckBox directorySchema = new CheckBox(msg.getMessage("ImportExport.directorySchema"));
		configBinder.forField(directorySchema).bind("directorySchema");

		CheckBox users = new CheckBox(msg.getMessage("ImportExport.users"));
		configBinder.forField(users).bind("users");
		CheckBox signupRequests = new CheckBox(msg.getMessage("ImportExport.signupRequests"));
		configBinder.forField(signupRequests).bind("signupRequests");
		
		CheckBox auditLogs = new CheckBox(msg.getMessage("ImportExport.auditLogs"));
		configBinder.forField(auditLogs).bind("auditLogs");
		
		CheckBox idpStatistics = new CheckBox(msg.getMessage("ImportExport.idpStatistics"));
		configBinder.forField(idpStatistics).bind("idpStatistics");
		
		users.addValueChangeListener(v -> {
			if (v.getValue())
			{
				directorySchema.setValue(true);;
			}
		});
		
		signupRequests.addValueChangeListener(v -> {
			if (v.getValue())
			{
				directorySchema.setValue(true);;
			}
		});
		
		directorySchema.addValueChangeListener(v -> {
			if (!v.getValue())
			{
				users.setValue(false);
				signupRequests.setValue(false);
			}
		});
		
		hl.addComponent(systemConfig);
		hl.addComponent(directorySchema);		
		HorizontalLayout usersWrapper = new HorizontalLayout();
		usersWrapper.setMargin(new MarginInfo(false, true));
		usersWrapper.setSpacing(true);
		usersWrapper.addComponent(users);
		
		HorizontalLayout signupWrapper = new HorizontalLayout();
		signupWrapper.setMargin(new MarginInfo(false, true));
		signupWrapper.setSpacing(true);
		signupWrapper.addComponent(signupRequests);
	
		hl.addComponent(usersWrapper);
		hl.addComponent(signupWrapper);
		hl.addComponent(auditLogs);	
		hl.addComponent(idpStatistics);	
		
		final DBDumpResource dumpResource = new DBDumpResource(serverManagement);
		DeletingFileDownloader downloader = new DeletingFileDownloader(dumpResource);
		Button createDump = new Button(msg.getMessage("ImportExport.createDump"));
		downloader.extend(createDump);

		hl.addComponents(createDump);

		configBinder.addStatusChangeListener( v -> {
			BinderDBDumpContentElements type = (BinderDBDumpContentElements) v.getBinder().getBean();
			createDump.setEnabled(type.isSystemConfig() || type.isDirectorySchema() || type.isUsers() || type.isAuditLogs() || type.isSignupRequests());
			dumpResource.setExportContent(new DBDumpContentElements(type.isSystemConfig(), type.isDirectorySchema(), type.isUsers(), type.isAuditLogs(), type.isSignupRequests(), type.isIdpStatistics()));
		});
		configBinder.setBean(new BinderDBDumpContentElements());
		
		createDump.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				//ugly as hell. we need to wait for the dump resource thread to start working
				//later on we are synced. If we start earlier and there is an error 
				//-> user will get neither stream nor error. Rather won't ever happen, but anyway...
				try
				{
					Thread.sleep(400);
				} catch (Exception e) {/*ignored*/}
				
				Exception e = dumpResource.getError();
				if (e != null)
				{
					NotificationPopup.showError(msg, msg.getMessage("ImportExport.exportFailed"), e);
					dumpResource.clearError();
				}
			}
		});
		addComponent(exportPanel);
	}

	private void initImportUI()
	{
		Panel importPanel = new SafePanel(msg.getMessage("ImportExport.importCaption"));
		VerticalLayout vl = new VerticalLayout();
		vl.setSpacing(true);
		vl.setMargin(true);
		importPanel.setContent(vl);

		Label info = new Label(msg.getMessage("ImportExport.uploadInfo"));
		Label fileUploaded = new Label(msg.getMessage("ImportExport.noFileUploaded"));
		ProgressBar progress = new ProgressBar();
		progress.setVisible(false);
		Upload upload = new Upload();
		upload.setCaption(msg.getMessage("ImportExport.uploadCaption"));
		uploader = new DumpUploader(upload, progress, fileUploaded);
		uploader.register();

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
			NotificationPopup.showError(msg.getMessage("error"),
					msg.getMessage("ImportExport.uploadFileTooBig"));
			return;
		}
		if (uploader.isUploading())
		{
			NotificationPopup.showError(msg.getMessage("error"),
					msg.getMessage("ImportExport.uploadInProgress"));
			return;
		}

		final File f = uploader.getFile();
		if (f == null)
		{
			NotificationPopup.showError(msg.getMessage("error"),
					msg.getMessage("ImportExport.uploadFileFirst"));
			return;
		}

		ConfirmDialog confirm = new ConfirmDialog(msg,
				msg.getMessage("ImportExport.confirm"), new Callback()
		{
			@Override
			public void onConfirm()
			{
				importDump(f);
			}
		});
		confirm.setHTMLContent(true);
		confirm.setSizeEm(50, 30);
		confirm.show();
	}

	private void importDump(File from)
	{
		try
		{
			Page.getCurrent().getJavaScript().execute("window.location.reload();");
			serverManagement.importDb(from);
		} catch (Exception e)
		{
			uploader.unblock();
			NotificationPopup.showError(msg, msg.getMessage("ImportExport.importFailed"), e);
		}
	}

	private class DumpUploader extends AbstractUploadReceiver
	{
		private Label info;

		private File target;

		private LimitedOuputStream fos;

		private boolean uploading = false;

		private boolean blocked = false;

		public DumpUploader(Upload upload, ProgressBar progress, Label info)
		{
			super(upload, progress);
			this.info = info;
		}

		@Override
		public synchronized void uploadSucceeded(SucceededEvent event)
		{
			super.uploadSucceeded(event);
			if (!fos.isOverflow() && fos.getLength() > 0)
				info.setValue(msg.getMessage("ImportExport.dumpUploaded",
						new Date()));
			else if (fos.getLength() == 0)
				info.setValue(msg.getMessage("ImportExport.uploadFileEmpty"));
			else
				info.setValue(msg.getMessage("ImportExport.uploadFileTooBig"));
			setUploading(false);
		}

		@Override
		public synchronized OutputStream receiveUpload(String filename, String mimeType)
		{
			if (blocked || uploading)
				throw new IllegalStateException(
						"Can't upload when update is in progress");
			try
			{
				if (target != null && target.exists())
					target.delete();
				target = createImportFile();
				setUploading(true);
				fos = new LimitedOuputStream(getDBDumbFileSizeLimit(), new BufferedOutputStream(
						new FileOutputStream(target)));
				return fos;
			} catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}

		private int getDBDumbFileSizeLimit()
		{
			Optional<Integer> dbBackupFileSizeLimit = serverConfig.getDBBackupFileSizeLimit();
			if (dbBackupFileSizeLimit.isPresent())
			{
				log.trace("Set static db dump file size limit to " + dbBackupFileSizeLimit.get());
				return dbBackupFileSizeLimit.get();
			}
			
			return calculateFileSizeLimitBasedOnFreeMemory();	
		}
		
		private int calculateFileSizeLimitBasedOnFreeMemory()
		{
			System.gc();
			long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			long filesizeLimit = ((Runtime.getRuntime().maxMemory() - initialMemory) * MAX_OF_FREE_MEMORY_USAGE_IN_PERCENT)/100;
			log.trace("Calculated dynamic db dumb file size limit: " + filesizeLimit);
			return (int)filesizeLimit;
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
		File workspace = serverConfig.getFileValue(
				UnityServerConfiguration.WORKSPACE_DIRECTORY, true);
		File importDir = new File(workspace, ServerManagement.DB_IMPORT_DIRECTORY);
		if (!importDir.exists())
			importDir.mkdir();
		File ret = new File(importDir, "databaseDump-uploaded.json");
		if (ret.exists())
			ret.delete();
		return ret;
	}

	/**
	 * Extension of {@link FileDownloader} which deletes the file after it is sent
	 * @author K. Benedyczak
	 */
	private class DeletingFileDownloader extends FileDownloader
	{
		private DBDumpResource dumpedFileResource;
		
		public DeletingFileDownloader(DBDumpResource resource)
		{
			super(resource);
			this.dumpedFileResource = resource;
		}

		@Override
		public boolean handleConnectorRequest(VaadinRequest request,
				VaadinResponse response, String path) throws IOException 
		{
			boolean ret = super.handleConnectorRequest(request, response, path);
			File dumped = dumpedFileResource.getFile();
			if (ret && dumped != null && dumped.exists())
			{
				dumped.delete();
			}
			return ret;
		}
	}
	
	private static class DBDumpResource extends FileResource
	{
		private final ServerManagement serverManagement;
		private File dump;
		private String filename;
		private Exception error;
		private DBDumpContentElements content;

		public DBDumpResource(ServerManagement serverManagement)
		{
			super(new File(getNewFilename()));
			this.serverManagement = serverManagement;
			filename = super.getFilename();
		}

		public void setExportContent(DBDumpContentElements value)
		{
			this.content = value;
			
		}

		@Override
		public DownloadStream getStream()
		{
			synchronized(this)
			{
				try
				{
					dump = serverManagement.exportDb(content);
					clearError();
				} catch (Exception e1)
				{
					setError(e1);
					return new DownloadStream(null, getMIMEType(), "missing.json");
				}
			}

			try 
			{
				filename = getNewFilename();
				final DownloadStream ds = new DownloadStream(new FileInputStream(dump), 
						getMIMEType(), filename);
				ds.setParameter("Content-Length", String.valueOf(dump.length()));
				ds.setCacheTime(0);
				return ds;
			} catch (final FileNotFoundException e) 
			{
				throw new RuntimeException("File not found: " + dump.getName(), e);
			}
		}

		private static String getNewFilename()
		{
			String ts = new SimpleDateFormat("yyyyMMdd-HHmmssMM").format(new Date());
			return "unity-dbdump-" + ts + ".json";
		}
		
		@Override
		public String getFilename() 
		{
			return filename;
		}

		public File getSourceFile() 
		{
			return dump;
		}
		
		public synchronized File getFile()
		{
			return dump;
		}
		
		@Override
		public String getMIMEType() 
		{
			return "application/octet-stream;charset=UTF-8";
		}
		
		private synchronized void setError(Exception e)
		{
			this.error = e;
		}
		
		public synchronized Exception getError()
		{
			return error;
		}
		
		public synchronized void clearError()
		{
			error = null;
		}
	}
	
	
	
	public static class BinderDBDumpContentElements
	{
		private boolean systemConfig = true;
		private boolean directorySchema = true;
		private boolean users = true;
		private boolean auditLogs = true;
		private boolean signupRequests = true;
		private boolean idpStatistics = true;

		public BinderDBDumpContentElements()
		{

		}

		public boolean isSystemConfig()
		{
			return systemConfig;
		}

		public void setSystemConfig(boolean systemConfig)
		{
			this.systemConfig = systemConfig;
		}

		public boolean isDirectorySchema()
		{
			return directorySchema;
		}

		public void setDirectorySchema(boolean directorySchema)
		{
			this.directorySchema = directorySchema;
		}

		public boolean isUsers()
		{
			return users;
		}

		public void setUsers(boolean users)
		{
			this.users = users;
		}

		public boolean isAuditLogs()
		{
			return auditLogs;
		}

		public void setAuditLogs(boolean auditLogs)
		{
			this.auditLogs = auditLogs;
		}

		public boolean isSignupRequests()
		{
			return signupRequests;
		}

		public void setSignupRequests(boolean signupRequests)
		{
			this.signupRequests = signupRequests;
		}
		
		public boolean isIdpStatistics()
		{
			return idpStatistics;
		}

		public void setIdpStatistics(boolean idpStatistics)
		{
			this.idpStatistics = idpStatistics;
		}
	}
}
