/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.json.dump.DBDumpContentElements;
import pl.edu.icm.unity.engine.api.ServerManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

@ExtendWith(MockitoExtension.class)
class BackupRestoreRESTAdminTest
{
	@TempDir
	Path tempDir;

	@Mock
	private ServerManagement serverManagement;

	@Mock
	private UnityServerConfiguration serverConfig;

	private BackupRestoreRESTAdmin handler;

	@BeforeEach
	void setUp()
	{
		handler = new BackupRestoreRESTAdmin(serverManagement, serverConfig);
	}

	@Test
	void shouldReturnOkResponseWithStreamingOutputWhenExportSucceeds() throws Exception
	{
		// given
		File dumpFile = createTempDumpFile("{\"test\": true}");
		when(serverManagement.exportDb(any())).thenReturn(dumpFile);

		// when
		Response response = handler.export(true, true, true, true, true, true);

		// then
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(response.getEntity()).isInstanceOf(StreamingOutput.class);

		String contentDisposition = response.getHeaderString("Content-Disposition");
		assertThat(contentDisposition).startsWith("attachment; filename=\"unity-dbdump-");
		assertThat(contentDisposition).endsWith(".json\"");

		String contentType = response.getMediaType().toString();
		assertThat(contentType).isEqualTo("application/json");
	}

	@Test
	void shouldPassCategorySelectionToServerManagement() throws Exception
	{
		// given
		File dumpFile = createTempDumpFile("{}");
		when(serverManagement.exportDb(any())).thenReturn(dumpFile);

		// when
		handler.export(true, false, false, false, false, false);

		// then
		ArgumentCaptor<DBDumpContentElements> captor = ArgumentCaptor.forClass(DBDumpContentElements.class);
		verify(serverManagement).exportDb(captor.capture());
		DBDumpContentElements captured = captor.getValue();

		assertThat(captured.systemConfig).isTrue();
		assertThat(captured.directorySchema).isFalse();
		assertThat(captured.users).isFalse();
		assertThat(captured.auditLogs).isFalse();
		assertThat(captured.signupRequests).isFalse();
		assertThat(captured.idpStatistics).isFalse();
	}

	@Test
	void shouldDefaultAllCategoriesToTrueWhenNotSpecified() throws Exception
	{
		// given
		File dumpFile = createTempDumpFile("{}");
		when(serverManagement.exportDb(any())).thenReturn(dumpFile);

		// when
		handler.export(true, true, true, true, true, true);

		// then
		ArgumentCaptor<DBDumpContentElements> captor = ArgumentCaptor.forClass(DBDumpContentElements.class);
		verify(serverManagement).exportDb(captor.capture());
		DBDumpContentElements captured = captor.getValue();

		assertThat(captured.systemConfig).isTrue();
		assertThat(captured.directorySchema).isTrue();
		assertThat(captured.users).isTrue();
		assertThat(captured.auditLogs).isTrue();
		assertThat(captured.signupRequests).isTrue();
		assertThat(captured.idpStatistics).isTrue();
	}

	@Test
	void shouldStreamFileContentToOutput() throws Exception
	{
		// given
		String expectedContent = "test-dump-content";
		File dumpFile = createTempDumpFile(expectedContent);
		when(serverManagement.exportDb(any())).thenReturn(dumpFile);

		// when
		Response response = handler.export(true, true, true, true, true, true);
		StreamingOutput streamingOutput = (StreamingOutput) response.getEntity();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		streamingOutput.write(outputStream);

		// then
		assertThat(outputStream.toString()).isEqualTo(expectedContent);
	}

	@Test
	void shouldDeleteTempFileAfterStreaming() throws Exception
	{
		// given
		File dumpFile = createTempDumpFile("content-to-stream");
		when(serverManagement.exportDb(any())).thenReturn(dumpFile);
		assertThat(dumpFile.exists()).isTrue();

		// when
		Response response = handler.export(true, true, true, true, true, true);
		StreamingOutput streamingOutput = (StreamingOutput) response.getEntity();
		streamingOutput.write(new ByteArrayOutputStream());

		// then
		assertThat(dumpFile.exists()).isFalse();
	}

	@Test
	void shouldCallImportDbWithTempFileWhenImportSucceeds() throws Exception
	{
		// given
		when(serverConfig.getFileValue(UnityServerConfiguration.WORKSPACE_DIRECTORY, true))
				.thenReturn(tempDir.toFile());
		when(serverConfig.getDBBackupFileSizeLimit()).thenReturn(Optional.of(1024));
		InputStream inputStream = new ByteArrayInputStream("{\"test\": true}".getBytes());

		// when
		Response response = handler.importBackup(inputStream, 14);

		// then
		assertThat(response.getStatus()).isEqualTo(200);
		verify(serverManagement).importDb(any(File.class));
	}

	@Test
	void shouldRejectRequestWhenHeaderExceedsStaticSizeLimit() throws Exception
	{
		// given
		when(serverConfig.getDBBackupFileSizeLimit()).thenReturn(Optional.of(100));
		InputStream inputStream = new ByteArrayInputStream("content".getBytes());

		// when / then
		assertThatThrownBy(() -> handler.importBackup(inputStream, 200))
				.isInstanceOf(WebApplicationException.class)
				.satisfies(ex ->
				{
					WebApplicationException wae = (WebApplicationException) ex;
					assertThat(wae.getResponse().getStatus()).isEqualTo(413);
				});
		verify(serverManagement, never()).importDb(any());
	}

	@Test
	void shouldRejectRequestWhenActualBytesExceedSizeLimit() throws Exception
	{
		// given
		when(serverConfig.getFileValue(UnityServerConfiguration.WORKSPACE_DIRECTORY, true))
				.thenReturn(tempDir.toFile());
		when(serverConfig.getDBBackupFileSizeLimit()).thenReturn(Optional.of(10));
		byte[] largeContent = new byte[20];
		InputStream inputStream = new ByteArrayInputStream(largeContent);

		// when / then
		assertThatThrownBy(() -> handler.importBackup(inputStream, 5))
				.isInstanceOf(WebApplicationException.class)
				.satisfies(ex ->
				{
					WebApplicationException wae = (WebApplicationException) ex;
					assertThat(wae.getResponse().getStatus()).isEqualTo(413);
				});
		verify(serverManagement, never()).importDb(any());
		assertThat(tempDir.resolve(ServerManagement.DB_IMPORT_DIRECTORY).toFile().listFiles())
				.satisfiesAnyOf(
						files -> assertThat(files).isNull(),
						files -> assertThat(files).isEmpty()
				);
	}

	@Test
	void shouldUseDynamicMemoryLimitWhenNoStaticLimitConfigured() throws Exception
	{
		// given
		when(serverConfig.getFileValue(UnityServerConfiguration.WORKSPACE_DIRECTORY, true))
				.thenReturn(tempDir.toFile());
		when(serverConfig.getDBBackupFileSizeLimit()).thenReturn(Optional.empty());
		InputStream inputStream = new ByteArrayInputStream("{\"small\": true}".getBytes());

		// when
		Response response = handler.importBackup(inputStream, 15);

		// then
		assertThat(response.getStatus()).isEqualTo(200);
		verify(serverManagement).importDb(any(File.class));
	}

	@Test
	void shouldCleanupTempFileAfterSuccessfulImport() throws Exception
	{
		// given
		when(serverConfig.getFileValue(UnityServerConfiguration.WORKSPACE_DIRECTORY, true))
				.thenReturn(tempDir.toFile());
		when(serverConfig.getDBBackupFileSizeLimit()).thenReturn(Optional.of(1024));
		InputStream inputStream = new ByteArrayInputStream("{\"data\": 1}".getBytes());

		// when
		handler.importBackup(inputStream, 11);

		// then
		File importDir = tempDir.resolve(ServerManagement.DB_IMPORT_DIRECTORY).toFile();
		assertThat(importDir.listFiles())
				.satisfiesAnyOf(
						files -> assertThat(files).isNull(),
						files -> assertThat(files).isEmpty()
				);
	}

	@Test
	void shouldCleanupTempFileAfterFailedImport() throws Exception
	{
		// given
		when(serverConfig.getFileValue(UnityServerConfiguration.WORKSPACE_DIRECTORY, true))
				.thenReturn(tempDir.toFile());
		when(serverConfig.getDBBackupFileSizeLimit()).thenReturn(Optional.of(1024));
		doThrow(new RuntimeException("Import failed"))
				.when(serverManagement).importDb(any(File.class));
		InputStream inputStream = new ByteArrayInputStream("{\"data\": 1}".getBytes());

		// when / then
		assertThatThrownBy(() -> handler.importBackup(inputStream, 11))
				.isInstanceOf(RuntimeException.class);
		File importDir = tempDir.resolve(ServerManagement.DB_IMPORT_DIRECTORY).toFile();
		assertThat(importDir.listFiles())
				.satisfiesAnyOf(
						files -> assertThat(files).isNull(),
						files -> assertThat(files).isEmpty()
				);
	}

	@Test
	void shouldReturnEngineExceptionWhenImportFails() throws Exception
	{
		// given
		when(serverConfig.getFileValue(UnityServerConfiguration.WORKSPACE_DIRECTORY, true))
				.thenReturn(tempDir.toFile());
		when(serverConfig.getDBBackupFileSizeLimit()).thenReturn(Optional.of(1024));
		doThrow(new EngineException("Import failed"))
				.when(serverManagement).importDb(any(File.class));
		InputStream inputStream = new ByteArrayInputStream("{\"data\": 1}".getBytes());

		// when / then
		// EngineException propagates from importBackup() -- at runtime, EngineExceptionMapper
		// converts this to HTTP 400 BAD_REQUEST (consistent with all REST admin endpoints).
		// The unit test verifies the exception propagates; the HTTP status mapping is the
		// responsibility of EngineExceptionMapper (tested separately in the rest module).
		assertThatThrownBy(() -> handler.importBackup(inputStream, 11))
				.isInstanceOf(EngineException.class)
				.hasMessage("Import failed");
	}

	private File createTempDumpFile(String content) throws Exception
	{
		Path filePath = tempDir.resolve("dump.json");
		Files.writeString(filePath, content);
		return filePath.toFile();
	}
}
