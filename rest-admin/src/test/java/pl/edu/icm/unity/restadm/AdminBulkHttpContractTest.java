package pl.edu.icm.unity.restadm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pl.edu.icm.unity.engine.api.bulk.GroupsWithMembers;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

class AdminBulkHttpContractTest extends AdminTransportHttpTestBase
{
	@TempDir
	Path workspace;

	@Test
	void shouldPreserveBulkContainersAndFilterErrors() throws Exception
	{
		when(bulkQueryService.getMembersWithAttributeForAllGroups(anyString(), any()))
				.thenReturn(new GroupsWithMembers(Map.of(), Map.of()));

		assertJson("/v1/group-members/root", "[]");
		var result = exchange("POST", "/v1/group-members-multi/root", "[\"/root/child\"]", "application/json");
		assertThat(result.statusCode()).as(result.body()).isEqualTo(200);
		assertThat(json.readTree(result.body())).isEqualTo(json.readTree("{\"entities\":[],\"members\":{}}"));
		var invalid = exchange("POST", "/v1/group-members-multi/root", "{", "application/json");
		assertThat(invalid.statusCode()).as(invalid.body()).isEqualTo(400);
		assertThat(json.readTree(invalid.body())).isEqualTo(json.readTree("""
				{"message":"Can not parse request body as a list of groups","error":"WrongArgumentException"}
				"""));
	}

	@Test
	void shouldStreamBackupBytesAndDeleteTheTemporaryExport() throws Exception
	{
		Path dump = workspace.resolve("dump.json");
		Files.writeString(dump, "{\"version\":123,\"data\":[\"żółć\"]}");
		when(serverManagement.exportDb(any())).thenReturn(dump.toFile());

		var response = exchange("GET", "/v1/db-dump", null, null);

		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		assertThat(response.headers().firstValue("content-disposition").orElse(""))
				.startsWith("attachment; filename=\"unity-dbdump-").endsWith(".json\"");
		assertThat(response.body()).isEqualTo("{\"version\":123,\"data\":[\"żółć\"]}");
		assertThat(dump).doesNotExist();
	}

	@Test
	void shouldKeepBackupImportStatusAndSizeLimit() throws Exception
	{
		when(serverConfig.getDBBackupFileSizeLimit()).thenReturn(Optional.of(20));
		when(serverConfig.getFileValue(UnityServerConfiguration.WORKSPACE_DIRECTORY, true))
				.thenReturn(workspace.toFile());

		var response = exchange("POST", "/v1/db-dump", "{}", "application/json");

		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		assertThat(response.body()).isEmpty();
		verify(serverManagement).importDb(any());
		var oversized = exchange("POST", "/v1/db-dump", "{\"value\":\"12345678901234567890\"}", "application/json");
		assertThat(oversized.statusCode()).isEqualTo(413);
	}
}
