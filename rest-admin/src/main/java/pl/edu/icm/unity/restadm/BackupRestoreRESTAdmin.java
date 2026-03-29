/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.json.dump.DBDumpContentElements;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

@Produces(MediaType.APPLICATION_JSON)
@Path(RESTAdminEndpoint.V1_PATH)
@PrototypeComponent
public class BackupRestoreRESTAdmin implements RESTAdminHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST,
			BackupRestoreRESTAdmin.class);
	private static final String DUMP_FILENAME_PREFIX = "unity-dbdump-";
	private static final int MAX_OF_FREE_MEMORY_USAGE_IN_PERCENT = 70;
	private static final int COPY_BUFFER_SIZE = 8192;

	private final ServerManagement serverManagement;
	private final UnityServerConfiguration serverConfig;

	@Autowired
	BackupRestoreRESTAdmin(ServerManagement serverManagement, UnityServerConfiguration serverConfig)
	{
		this.serverManagement = serverManagement;
		this.serverConfig = serverConfig;
	}

	@GET
	@Path("/export")
	public Response export(
			@QueryParam("systemConfig") @DefaultValue("true") boolean systemConfig,
			@QueryParam("directorySchema") @DefaultValue("true") boolean directorySchema,
			@QueryParam("users") @DefaultValue("true") boolean users,
			@QueryParam("auditLogs") @DefaultValue("true") boolean auditLogs,
			@QueryParam("signupRequests") @DefaultValue("true") boolean signupRequests,
			@QueryParam("idpStatistics") @DefaultValue("true") boolean idpStatistics)
			throws EngineException
	{
		log.info("Database export requested");
		DBDumpContentElements content = new DBDumpContentElements(
				systemConfig, directorySchema, users, auditLogs,
				signupRequests, idpStatistics);
		File dumpFile = serverManagement.exportDb(content);
		String filename = createTimestampedFilename();
		log.info("Streaming database export as {}", filename);

		StreamingOutput stream = output ->
		{
			try (InputStream is = new BufferedInputStream(new FileInputStream(dumpFile)))
			{
				is.transferTo(output);
			}
			finally
			{
				if (!dumpFile.delete())
				{
					log.warn("Failed to delete temporary export file: {}", dumpFile.getAbsolutePath());
				}
			}
		};

		return Response.ok(stream, MediaType.APPLICATION_JSON)
				.header("Content-Disposition",
						"attachment; filename=\"" + filename + "\"")
				.build();
	}

	private String createTimestampedFilename()
	{
		String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss")
				.format(new Date());
		return DUMP_FILENAME_PREFIX + timestamp + ".json";
	}

	@POST
	@Path("/import")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response importBackup(InputStream inputStream,
			@HeaderParam("Content-Length") long contentLength) throws EngineException
	{
		long sizeLimit = getFileSizeLimit();
		validateContentLengthHeader(contentLength, sizeLimit);

		File file = null;
		try
		{
			file = createImportFile();
			copyWithSizeLimit(inputStream, file, sizeLimit);
			log.warn("Database import starting — endpoint will restart, client may not receive response");
			serverManagement.importDb(file);
			return Response.ok().build();
		}
		catch (IOException e)
		{
			log.error("Failed to process import file", e);
			throw new WebApplicationException("Failed to process import file",
					Status.INTERNAL_SERVER_ERROR);
		}
		finally
		{
			if (file != null && file.exists() && !file.delete())
			{
				log.warn("Failed to delete temporary import file: {}", file.getAbsolutePath());
			}
		}
	}

	private void validateContentLengthHeader(long contentLength, long limit)
	{
		if (contentLength <= 0)
		{
			return;
		}
		if (contentLength > limit)
		{
			log.info("Import rejected: Content-Length header {} exceeds limit {}", contentLength, limit);
			throw new WebApplicationException("Upload size " + contentLength
					+ " exceeds maximum allowed size " + limit,
					Status.REQUEST_ENTITY_TOO_LARGE);
		}
	}

	private void copyWithSizeLimit(InputStream input, File destination, long limit) throws IOException
	{
		long totalBytesWritten = 0;
		byte[] buffer = new byte[COPY_BUFFER_SIZE];
		try (OutputStream output = new FileOutputStream(destination))
		{
			int bytesRead;
			while ((bytesRead = input.read(buffer)) != -1)
			{
				totalBytesWritten += bytesRead;
				if (totalBytesWritten > limit)
				{
					log.info("Import rejected: actual bytes written {} exceed limit {} "
							+ "(Content-Length header may have been spoofed)",
							totalBytesWritten, limit);
					throw new WebApplicationException(
							"Upload size exceeds maximum allowed size " + limit,
							Status.REQUEST_ENTITY_TOO_LARGE);
				}
				output.write(buffer, 0, bytesRead);
			}
		}
	}

	private long getFileSizeLimit()
	{
		Optional<Integer> staticLimit = serverConfig.getDBBackupFileSizeLimit();
		if (staticLimit.isPresent())
		{
			log.trace("Using static db backup file size limit: {}", staticLimit.get());
			return staticLimit.get();
		}
		return calculateFileSizeLimitBasedOnFreeMemory();
	}

	private long calculateFileSizeLimitBasedOnFreeMemory()
	{
		System.gc();
		long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		long filesizeLimit = ((Runtime.getRuntime().maxMemory() - initialMemory)
				* MAX_OF_FREE_MEMORY_USAGE_IN_PERCENT) / 100;
		log.trace("Calculated dynamic db backup file size limit: {}", filesizeLimit);
		return filesizeLimit;
	}

	private File createImportFile() throws IOException
	{
		File workspace = serverConfig.getFileValue(
				UnityServerConfiguration.WORKSPACE_DIRECTORY, true);
		File importDir = new File(workspace, ServerManagement.DB_IMPORT_DIRECTORY);
		if (!importDir.exists())
		{
			importDir.mkdirs();
		}
		return new File(importDir, "databaseDump-" + UUID.randomUUID() + ".json");
	}
}
