# REST API documentation

From the repository root, generate and validate both the Admin and UpMan references without starting Unity:

```sh
mvn -pl unity-rest-api-docs -am verify -DskipTests -Dgpg.skip=true
```

The exporter runs during `prepare-package`, including when tests are skipped. It requires no database,
configured endpoint, listening port, Node installation or separately installed renderer. The normal reactor
`verify`/`install` lifecycle runs it too; no separate CI job is required. No CI configuration is tracked here.

Output in `unity-rest-api-docs/target/openapi`:

- `admin-openapi-v1.yaml`: deterministic Administration API v1 specification.
- `admin-openapi-v1.html`: offline Administration API reference with the specification embedded.
- `upman-openapi-v1.yaml`: deterministic OpenAPI v1 specification.
- `upman-openapi-v1.html`: offline reference with the specification embedded.
- `openapi-assets/`: Redoc standalone JavaScript and its license notice from the version pinned in the root POM.

Keep the HTML and assets together; the Redoc HTML can be opened directly from disk without a network connection.
The specification is embedded and fonts use local system fonts. This published reference is for reading;
configure the actual deployed endpoint URL in your API client.

## Live documentation

Open `api-docs` beneath a deployed REST endpoint, for example `/restupm/api-docs`, to use Scalar.
The HTML, bundled JavaScript and live `openapi.yaml` / `openapi.json` retain the endpoint's authentication.
The UI loads the current endpoint's specification and sends requests directly to that same endpoint, including
when deployed beneath a custom path. No renderer, font or request proxy is loaded from a CDN. Scalar's bundled
WebJar is a runtime dependency of `rest` and is included in the distribution's `lib` directory.

Authenticate the documentation using an authenticator configured for that endpoint. For request execution,
select the specification's Basic or Bearer authentication scheme and enter the corresponding credentials.
UpMan bearer tokens need the documented API scopes. OAuth issuer and token endpoints are deployment configuration;
the generic Bearer field accepts a token obtained from your configured issuer. Credentials entered in Scalar are
not persisted across page reloads. An authenticated specification download alone does not verify request execution:
use **Test Request** on `GET /v1/projects` (UpMan) or `GET /v1/policy-documents` (Admin), submit the request
and inspect the returned status and response body.

`unity.endpoint.rest.enableOpenApi=true` is the shared default. Set it to `false` in an individual endpoint's
configuration to disable its live specification, Scalar HTML and assets. This setting applies independently to
Admin, UpMan and other REST endpoint instances. API routes and build-time/site YAML and Redoc generation remain
available. Swagger UI is disabled for live endpoints.

The `documentation` module invokes the same exporter during `pre-site` and places the generated
references beside the handwritten guides and workflow documents. After installing reactor dependencies, build
the site from the repository root with `mvn -pl documentation compile site -Dgpg.skip=true` and the usual
Asciidoctor executable on `PATH`. API version `1` and the Unity product release version are separate.

## Validation and maintenance

Run the documentation and production schema checks with:

```sh
mvn -pl unity-rest-api-docs -am test \
  '-Dtest=OpenApiExporterTest,OpenApiIsolationTest,*OpenApiTest,*OpenApiHttpContractTest' \
  -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true -Dhttp.proxyHost= -Dhttps.proxyHost=
```

Generation rejects invalid OpenAPI, unresolved references, duplicate operation IDs, opaque object bodies,
and examples that fail their schemas. The checked-in HTTP route inventories cover all 51 UpMan and 80 Admin
operations;
production tests compare the authenticated live document to the offline document and preserve literal wire
fixtures. The complete `web-upman` and `rest-admin` suites and repository headless verification remain regression gates.
The isolation tests compare both live specifications with their exports in both endpoint initialization orders,
check that global model converters stay unchanged, and exercise per-endpoint publication settings.

Review semantic changes to paths, media types, statuses, required fields, enums, discriminators and security
when API code changes. Compare generated YAML from the base revision with the branch output; property ordering
is deterministic. Do not edit generated YAML or HTML.

Maintain operation explanations in
`web-upman/src/main/resources/io/imunity/upman/rest/openapi/operations.properties`.
Literal examples in the adjacent `contract` directory are shared with the HTTP tests and remain independent
of DTO serialization.

The Admin operation explanations are maintained in
`rest-admin/src/main/resources/io/imunity/rest/admin/openapi/operations.properties`, with independent HTTP
fixtures in the adjacent `contract` directory. Admin timestamps, flattened invitations and request states,
plugin payloads, error ordering and media types retain their existing contracts. See
`rest-admin/src/test/java/pl/edu/icm/unity/restadm/HTTP_CONTRACT_COVERAGE.md` for the route coverage.

The former `-PopenapiSpec` profile is retained as a compatibility alias. Its fixed-port server bootstrap and
specification download have been replaced by the normal build-time exporter for both APIs.
