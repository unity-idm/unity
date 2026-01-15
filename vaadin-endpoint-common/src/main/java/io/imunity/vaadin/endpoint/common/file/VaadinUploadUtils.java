/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.file;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.InMemoryUploadHandler;
import com.vaadin.flow.server.streams.TemporaryFileUploadHandler;
import com.vaadin.flow.server.streams.UploadHandler;

/**
 * Utility class for creating {@link Upload} components with the new {@link UploadHandler} API.
 * Replaces deprecated {@code Receiver}, {@code MemoryBuffer}, {@code FileBuffer} APIs.
 * 
 * <p>Usage examples:</p>
 * <pre>
 * // Simple in-memory upload
 * Upload upload = VaadinUploadUtils.createInMemoryUpload((fileName, bytes) -> {
 *     processUploadedFile(fileName, bytes);
 * });
 * 
 * // Upload to temp file
 * Upload upload = VaadinUploadUtils.createTempFileUpload((fileName, file) -> {
 *     processFile(file);
 * });
 * </pre>
 */
public final class VaadinUploadUtils
{
	private VaadinUploadUtils()
	{
	}

	/**
	 * Creates an Upload component configured for in-memory file handling.
	 * The callback receives the filename and uploaded bytes after successful upload.
	 * UI updates in the callback are safe (automatically wrapped in UI.access).
	 *
	 * @param onUploadComplete callback receiving (fileName, bytes) after successful upload
	 * @return configured Upload component
	 */
	public static Upload createInMemoryUpload(BiConsumer<String, byte[]> onUploadComplete)
	{
		return new Upload(new InMemoryUploadHandler((metadata, bytes) ->
			onUploadComplete.accept(metadata.fileName(), bytes)
		));
	}

	/**
	 * Creates an Upload component configured for in-memory file handling with error callback.
	 *
	 * @param onUploadComplete callback receiving (fileName, bytes) after successful upload
	 * @param onError          callback receiving the exception on error
	 * @return configured Upload component
	 */
	public static Upload createInMemoryUpload(
		BiConsumer<String, byte[]> onUploadComplete,
		Consumer<Exception> onError)
	{
		return new Upload(event ->
		{
			UI ui = event.getUI();
			try (InputStream is = event.getInputStream())
			{
				byte[] bytes = readAllBytes(is);
				String fileName = event.getFileName();
				ui.access(() -> onUploadComplete.accept(fileName, bytes));
			}
			catch (Exception e)
			{
				ui.access(() -> onError.accept(e));
			}
		});
	}

	/**
	 * Creates an Upload component configured for temporary file handling.
	 * The callback receives the filename and temporary file after successful upload.
	 * The temporary file should be processed and will be deleted after the callback completes.
	 * UI updates in the callback are safe (automatically wrapped in UI.access).
	 *
	 * @param onUploadComplete callback receiving (fileName, tempFile) after successful upload
	 * @return configured Upload component
	 */
	public static Upload createTempFileUpload(BiConsumer<String, File> onUploadComplete)
	{
		return new Upload(new TemporaryFileUploadHandler((metadata, file) ->
			onUploadComplete.accept(metadata.fileName(), file)
		));
	}

	/**
	 * Creates an UploadHandler for streaming processing.
	 * Use this when you need direct access to the InputStream for processing.
	 * Note: UI is not locked during processing; use UI.access() for UI updates.
	 *
	 * @param streamProcessor consumer that processes (fileName, inputStream)
	 * @return UploadHandler instance
	 */
	public static UploadHandler createStreamingHandler(BiConsumer<String, InputStream> streamProcessor)
	{
		return event ->
		{
			try (InputStream is = event.getInputStream())
			{
				streamProcessor.accept(event.getFileName(), is);
			}
		};
	}

	/**
	 * Creates an UploadHandler that writes to a provided OutputStream.
	 * Useful for custom storage implementations.
	 *
	 * @param outputStreamProvider provides the OutputStream for writing
	 * @param onComplete           called after successful transfer with (fileName, bytesWritten)
	 * @return UploadHandler instance
	 */
	public static UploadHandler createOutputStreamHandler(
		java.util.function.Supplier<OutputStream> outputStreamProvider,
		BiConsumer<String, Long> onComplete)
	{
		return event ->
		{
			String fileName = event.getFileName();
			try (InputStream is = event.getInputStream();
				 OutputStream os = outputStreamProvider.get())
			{
				long bytesWritten = transferTo(is, os);
				UI ui = event.getUI();
				ui.access(() -> onComplete.accept(fileName, bytesWritten));
			}
		};
	}

	/**
	 * Configures common upload settings on an existing Upload component.
	 *
	 * @param upload      the Upload component to configure
	 * @param maxFiles    maximum number of files (use 1 for single file)
	 * @param maxFileSize maximum file size in bytes
	 * @param mimeTypes   accepted MIME types (e.g., "image/*", "application/json")
	 */
	public static void configureUpload(Upload upload, int maxFiles, int maxFileSize, String... mimeTypes)
	{
		upload.setMaxFiles(maxFiles);
		upload.setMaxFileSize(maxFileSize);
		if (mimeTypes != null && mimeTypes.length > 0)
		{
			upload.setAcceptedFileTypes(mimeTypes);
		}
	}

	private static byte[] readAllBytes(InputStream is) throws IOException
	{
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int bytesRead;
		byte[] data = new byte[8192];
		while ((bytesRead = is.read(data, 0, data.length)) != -1)
		{
			buffer.write(data, 0, bytesRead);
		}
		return buffer.toByteArray();
	}

	private static long transferTo(InputStream is, OutputStream os) throws IOException
	{
		long transferred = 0;
		byte[] buffer = new byte[8192];
		int read;
		while ((read = is.read(buffer, 0, buffer.length)) >= 0)
		{
			os.write(buffer, 0, read);
			transferred += read;
		}
		return transferred;
	}
}
