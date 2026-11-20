# Admin HTTP contract coverage

The admin transport migration is characterized separately from UpMan. `AdminHttpContractTestBase`
uses the existing authenticated, database-backed endpoint. `AdminTransportHttpTestBase` uses mocked
engine dependencies with the production JAX-RS application, resources, JSON provider, Jetty/CXF
transport and exception mappers. It supplements the existing database-backed admin service tests.

Literal fixtures in `src/main/resources/io/imunity/rest/admin/contract` are compared as complete
JSON trees and reused by OpenAPI examples. They are independent of DTO serialization.

| Family | Transport evidence | Existing regression evidence |
| --- | --- | --- |
| Policies | `AdminPolicyHttpContractTest`: lifecycle, revisions, media types, malformed/null inputs, validation ordering | `TestPolicyDocumentManagement` |
| Directory | `AdminDirectoryHttpContractTest`: entity/attribute/group trees, encoded separators, legacy parse failures | `TestQuery`, `TestWrite`, `TestGroupsManagement` |
| Forms | `AdminFormHttpContractTest`: full registration/enquiry CRUD trees, inherited fields, layouts, parser/media failures | Existing registration/enquiry engine tests |
| Invitations and requests | `AdminWorkflowHttpContractTest`: flattened invitation/request trees, milliseconds, text invitation code | `TestInvitations` and engine invitation suites |
| Tokens and bulk processing | `AdminWorkflowHttpContractTest`: populated formatted contents and plain statuses | `TestTokens`, `TestBulkProcessing` |
| Bulk queries and backup | `AdminBulkHttpContractTest`: response containers, filter failures, exact stream bytes and import limits | `BackupRestoreRESTAdminTest`, `TestBackupRestore` |
| Endpoint configuration | `AdminEndpointHttpContractTest`: complete deployed/listed endpoint trees, including custom serialization | `TestEndpointsManagement` |
| Documentation | `AdminOpenApiHttpContractTest`: authenticated live/offline equality and authentication enforcement | `AdminOpenApiTest`, `OpenApiExporterTest`, `AdminOpenApiIsolationTest` |

The independent inventory in `src/test/resources/io/imunity/rest/admin/routes.txt` contains all
80 method/path pairs. `AdminOpenApiTest` checks these routes, unique operation IDs, descriptions,
explicit success/errors and flattened schema details. Export validation checks references,
nonopaque bodies and representative raw examples. Negative fixtures reject string timestamps
and policies with missing required fields. The isolation test runs two differently configured
UpMan applications and the full admin application in one process and checks global converters.

Important preserved behavior:

- Admin invitation timestamps are Unix milliseconds; UpMan invitation timestamps are seconds.
- Invitation responses flatten prefill and metadata. Request states flatten collected input and
  retain mixed-case fields. `CreatedEntityId` is omitted when absent.
- Malformed policy JSON returns the generic BadRequestException envelope. Malformed form JSON
  returns the IllegalArgumentException envelope. Group/attribute-type parser failures can return
  an empty 500. Missing incrementRevision is checked before policy update body parsing.
- Policy creation returns 200, void mutations return 204, invitation codes and bulk execution
  statuses are text/plain, and database backups retain streaming and attachment behavior.
- Runtime documentation is public when explicitly enabled. API requests without credentials still
  return the existing 400 Authentication failed envelope with the direct password authenticator.
- Credential-admin and event payloads remain raw plugin inputs. Token contents remain dynamic JSON.

Run the complete `rest-admin` suite and the repository headless full build as final regression gates.
These transport/schema changes do not alter persisted models and require no database migration.

Split validation: the `unity-rest-api-docs` reactor `clean install` completed with 2,980 tests
(four skipped, no failures or errors), including the entire unchanged UpMan suite.
The test-only baseline passed all 19 HTTP contract tests before JSON mapping.
The documentation `compile pre-site` lifecycle rendered the guide and generated reference,
with only admin HTML/YAML artifacts. The API-specific coexistence and deterministic export tests passed.
