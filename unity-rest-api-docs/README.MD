# UpMan REST API documentation

This branch contains the UpMan migration only and starts directly from `dev`. Its first commit adds
only HTTP contract tests and literal fixtures, before the JSON mapping commits. The baseline expectations
follow current `dev`, including null-email members and authorization/conflict error bodies. Additional
schema boundary checks accompany the migration. The admin migration is independent.

From the repository root, generate and validate the UpMan reference without starting Unity:

```sh
mvn -pl unity-rest-api-docs -am verify -DskipTests -Dgpg.skip=true
```

The exporter runs during `prepare-package`, including when tests are skipped. It requires no database,
configured endpoint, listening port, Node installation or separately installed renderer. The normal reactor
`verify`/`install` lifecycle runs it too; no separate CI job is required. No CI configuration is tracked here.

Output in `unity-rest-api-docs/target/openapi`:

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
use **Test Request** on `GET /v1/projects`, submit the request and inspect the returned status and response body.

`unity.endpoint.rest.enableOpenApi=true` is the shared default. Set it to `false` in an individual endpoint's
configuration to disable its live specification, Scalar HTML and assets. This setting applies independently to
Admin, UpMan and other REST endpoint instances. API routes and build-time/site YAML and Redoc generation remain
available. Swagger UI is disabled for live endpoints.

The existing `documentation` module invokes the same exporter during `pre-site` and places the generated
reference beside the handwritten context/workflow document. After installing reactor dependencies, build
the site from the repository root with `mvn -pl documentation compile pre-site -Dgpg.skip=true` and the usual
Asciidoctor executable on `PATH`. API version `1` and the Unity product release version are separate.

## Validation and maintenance

Run the documentation and production schema checks with:

```sh
mvn -pl unity-rest-api-docs -am test \
  '-Dtest=OpenApiExporterTest,UpmanOpenApiIsolationTest,UpmanOpenApiTest,UpmanOpenApiHttpContractTest' \
  -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true -Dhttp.proxyHost= -Dhttps.proxyHost=
```

Generation rejects invalid OpenAPI, unresolved references, duplicate operation IDs, opaque object bodies,
and examples that fail their schemas. The checked-in HTTP route inventory covers all 51 UpMan operations;
production tests compare the authenticated live document to the offline document and preserve literal wire
fixtures. The complete `web-upman` suite and repository headless verification remain regression gates.

Review semantic changes to paths, media types, statuses, required fields, enums, discriminators and security
when API code changes. Compare generated YAML from the base revision with the branch output; property ordering
is deterministic. Do not edit generated YAML or HTML.

Maintain operation explanations in
`web-upman/src/main/resources/io/imunity/upman/rest/openapi/operations.properties`.
Literal examples in the adjacent `contract` directory are shared with the HTTP tests and remain independent
of DTO serialization.

## Existing admin generation

The existing full-server admin exporter remains available with `-PopenapiSpec`. It still uses the legacy
startup configuration and generates `target/openapi.yaml`; the admin migration is preserved independently on
`UY-1424-admin-openapi-docs`. This branch leaves the admin production sources unchanged.
