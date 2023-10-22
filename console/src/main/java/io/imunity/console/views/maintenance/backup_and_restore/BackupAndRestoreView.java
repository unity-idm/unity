/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.maintenance.backup_and_restore;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.InputLabel;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.elements.Panel;
import jakarta.annotation.security.PermitAll;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.json.dump.DBDumpContentElements;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.imunity.vaadin.elements.CSSVars.BASE_MARGIN;
import static io.imunity.vaadin.elements.VaadinClassNames.SMALL_GAP;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.maintenance.backupAndRestore")
@Route(value = "/backup-and-restore", layout = ConsoleMenu.class)
public class BackupAndRestoreView extends ConsoleViewComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, BackupAndRestoreView.class);
	private static final int MAX_OF_FREE_MEMORY_USAGE_IN_PERCENT = 70;


	private final MessageSource msg;
	private final ServerManagement serverManagement;
	private final NotificationPresenter notificationPresenter;
	private final UnityServerConfiguration serverConfig;
	private final UI ui;
	private MemoryBuffer memoryBuffer;
	private Upload upload;

	BackupAndRestoreView(MessageSource msg, ServerManagement serverManagement,
						 UnityServerConfiguration serverConfig, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.serverManagement = serverManagement;
		this.notificationPresenter = notificationPresenter;
		this.serverConfig = serverConfig;
		this.ui = UI.getCurrent();
		init();
	}

	public void init()
	{
		getContent().removeAll();
		getContent().add(createExportUI());
		getContent().add(createImportUI());
	}

	private VerticalLayout createExportUI()
	{
		Panel exportPanel = new Panel(Optional.of(msg.getMessage("ImportExport.exportCaption")));
		VerticalLayout layout = new VerticalLayout();
		layout.setClassName(SMALL_GAP.getName());
		exportPanel.add(layout);

		Binder<BinderDBDumpContentElements> configBinder = new Binder<>(BinderDBDumpContentElements.class);

		Checkbox systemConfig = new Checkbox(msg.getMessage("ImportExport.systemConfig"));
		configBinder.forField(systemConfig)
				.bind(BinderDBDumpContentElements::isSystemConfig, BinderDBDumpContentElements::setSystemConfig);

		Checkbox directorySchema = new Checkbox(msg.getMessage("ImportExport.directorySchema"));
		configBinder.forField(directorySchema)
				.bind(BinderDBDumpContentElements::isDirectorySchema, BinderDBDumpContentElements::setDirectorySchema);

		Checkbox users = new Checkbox(msg.getMessage("ImportExport.users"));
		configBinder.forField(users)
				.bind(BinderDBDumpContentElements::isUsers, BinderDBDumpContentElements::setUsers);

		Checkbox signupRequests = new Checkbox(msg.getMessage("ImportExport.signupRequests"));
		configBinder.forField(signupRequests)
				.bind(BinderDBDumpContentElements::isSignupRequests, BinderDBDumpContentElements::setSignupRequests);

		Checkbox auditLogs = new Checkbox(msg.getMessage("ImportExport.auditLogs"));
		configBinder.forField(auditLogs)
				.bind(BinderDBDumpContentElements::isAuditLogs, BinderDBDumpContentElements::setAuditLogs);

		Checkbox idpStatistics = new Checkbox(msg.getMessage("ImportExport.idpStatistics"));
		configBinder.forField(idpStatistics)
				.bind(BinderDBDumpContentElements::isIdpStatistics, BinderDBDumpContentElements::setIdpStatistics);

		users.addValueChangeListener(v -> {
			if (v.getValue())
				directorySchema.setValue(true);
		});

		signupRequests.addValueChangeListener(v -> {
			if (v.getValue())
				directorySchema.setValue(true);
		});

		directorySchema.addValueChangeListener(v -> {
			if (!v.getValue())
			{
				users.setValue(false);
				signupRequests.setValue(false);
			}
		});

		layout.add(systemConfig, directorySchema, users, signupRequests, auditLogs, idpStatistics);
		users.getStyle().set("margin-left", BASE_MARGIN.value());
		signupRequests.getStyle().set("margin-left", BASE_MARGIN.value());

		Anchor download = new Anchor(getStreamResource(configBinder), "");
		download.getElement().setAttribute("download", true);
		download.removeAll();
		Button createDump = new Button(msg.getMessage("ImportExport.createDump"));
		download.add(createDump);
		layout.add(download);

		configBinder.addStatusChangeListener(event ->
		{
			BinderDBDumpContentElements type = (BinderDBDumpContentElements) event.getBinder().getBean();
			createDump.setEnabled(type.isSystemConfig() || type.isDirectorySchema() || type.isUsers() || type.isAuditLogs() || type.isSignupRequests());
		});
		configBinder.setBean(new BinderDBDumpContentElements());

		return exportPanel;
	}

	private StreamResource getStreamResource(Binder<BinderDBDumpContentElements> configBinder)
	{
		return new StreamResource(getNewFilename(),
				() ->
				{
					BinderDBDumpContentElements type = configBinder.getBean();
					DBDumpContentElements dbDumpContentElements =
							new DBDumpContentElements(type.isSystemConfig(), type.isDirectorySchema(), type.isUsers(),
									type.isAuditLogs(), type.isSignupRequests(), type.isIdpStatistics()
							);
					try
					{
						return new FileInputStream(serverManagement.exportDb(dbDumpContentElements));
					} catch (Exception e)
					{
						ui.access(() -> notificationPresenter.showError(msg.getMessage("ImportExport.exportFailed"), e.getMessage()));
						throw new RuntimeException(e);
					}
				})
		{
			@Override
			public Map<String, String> getHeaders()
			{
				Map<String, String> headers = new HashMap<>(super.getHeaders());
				headers.put("Content-Disposition", "attachment; filename=\"" + getNewFilename() + "\"");
				return headers;
			}
		};
	}

	private String getNewFilename()
	{
		String ts = new SimpleDateFormat("yyyyMMdd-HHmmssMM").format(new Date());
		return "unity-dbdump-" + ts + ".json";
	}

	private VerticalLayout createImportUI()
	{
		Panel importPanel = new Panel(Optional.of(msg.getMessage("ImportExport.importCaption")));
		VerticalLayout layout = new VerticalLayout();
		layout.setSpacing(false);
		importPanel.add(layout);

		Span info = new Span(msg.getMessage("ImportExport.uploadInfo"));
		Span fileUploaded = new Span(msg.getMessage("ImportExport.noFileUploaded"));
		fileUploaded.getStyle().set("margin", "var(--small-margin) 0");

		memoryBuffer = new MemoryBuffer();
		upload = new Upload(memoryBuffer);
		upload.setMaxFileSize(getDBDumbFileSizeLimit());
		upload.setAcceptedFileTypes("application/json");
		upload.addFinishedListener(e -> fileUploaded.setText(msg.getMessage("ImportExport.dumpUploaded", new Date())));
		upload.getElement().addEventListener("file-remove", e -> fileUploaded.setText(msg.getMessage("ImportExport.noFileUploaded")));
		upload.addFileRejectedListener(e -> notificationPresenter.showError(msg.getMessage("error"),
				e.getErrorMessage()));

		upload.setDropAllowed(false);
		upload.setWidth("21em");
		InputLabel inputLabel = new InputLabel(msg.getMessage("ImportExport.uploadCaption"));

		Button importDump = new Button(msg.getMessage("ImportExport.import"));
		importDump.addClickListener(e -> importDumpInit());

		layout.add(info, inputLabel, upload, fileUploaded, importDump);
		return importPanel;
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

	private void importDumpInit()
	{
		if (upload.isUploading())
		{
			notificationPresenter.showError(msg.getMessage("error"),
					msg.getMessage("ImportExport.uploadInProgress"));
			return;
		}

		try
		{
			if (memoryBuffer.getInputStream().available() == 0)
			{
				notificationPresenter.showError(msg.getMessage("error"),
						msg.getMessage("ImportExport.uploadFileFirst"));
				return;
			}
		}
		catch (IOException e)
		{
			notificationPresenter.showError(msg.getMessage("error"),
					msg.getMessage("ImportExport.uploadFileFirst"));
			log.error("Occurred while memoryBuffer was read", e);
			return;
		}

		ConfirmDialog confirm = new ConfirmDialog(
				msg.getMessage("ConfirmDialog.confirm"),
				msg.getMessage("ImportExport.confirm"),
				msg.getMessage("ok"),
				e -> importDump(copyInputStreamToFile(memoryBuffer.getInputStream())),
				msg.getMessage("cancel"),
				e -> {}
		);
		confirm.setText(new Html("<div>" + msg.getMessage("ImportExport.confirm") + "</div>"));
		confirm.setWidth("50em");
		confirm.open();
	}

	public File copyInputStreamToFile(InputStream input)
	{
		File file = createImportFile();
		try (OutputStream output = new FileOutputStream(file))
		{
			input.transferTo(output);
		}
		catch (IOException e)
		{
			log.error("Occurred while input stream was exported to file", e);
		}
		return file;
	}

	private File createImportFile()
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

	private void importDump(File from)
	{
		try
		{
			serverManagement.importDb(from);
		}
		catch (Exception e)
		{
			log.error("Occurred while db was imported from backup", e);
			ui.access(() -> notificationPresenter.showError(msg.getMessage("ImportExport.importFailed"), e.getMessage()));
		}
	}

}
