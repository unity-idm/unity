# Admin REST API documentation

This branch contains the admin API migration only. It is based on `dev` and can be released
independently of `UY-1424-upman-openapi-docs`. UpMan production sources remain unchanged.
The first commit, `009744ec03`, contains only HTTP syntax tests and literal fixtures.
All 19 tests pass against the original parser before the mapping commits.
Later commits introduce scoped JSON mapping, schema generation and publication.

From the repository root, generate and validate the admin reference without starting Unity:

```sh
mvn -pl unity-rest-api-docs -am verify -DskipTests -Dgpg.skip=true
```

Generation runs during `prepare-package`, including when tests are skipped. It requires no database,
configured endpoint, listening port, Node installation or separately installed renderer. The normal
reactor `verify`/`install` lifecycle runs the same validation, so no separate CI job is required.

Output in `unity-rest-api-docs/target/openapi`:

- `admin-openapi-v1.yaml`: deterministic OpenAPI API-version-1 specification.
- `admin-openapi-v1.html`: offline Swagger UI reference with the specification embedded.
- `openapi-assets/`: renderer JavaScript and CSS pinned by the root POM.

Keep the HTML and assets together. The published reference disables request execution. Live
`openapi.yaml` and `openapi.json` retain endpoint authentication. Configure the actual endpoint URL
in client tooling; API version 1 is independent of the Unity product release.

After installing reactor dependencies, render the existing documentation site from the repository
root with `mvn -pl documentation compile pre-site -Dgpg.skip=true` and the usual Asciidoctor executable on `PATH`.
The admin guide links to the generated HTML and YAML. Workspace paths are independent of shell location.

## Validation and maintenance

```sh
mvn -pl unity-rest-api-docs -am test \
  '-Dtest=OpenApiExporterTest,AdminOpenApiIsolationTest,AdminOpenApiTest,AdminOpenApiHttpContractTest' \
  -Dsurefire.failIfNoSpecifiedTests=false -Dgpg.skip=true -Dhttp.proxyHost= -Dhttps.proxyHost=
```

Generation rejects unresolved references, missing or duplicate operation IDs, unspecified object bodies,
missing explicit responses and examples that disagree with their schemas. The independent route inventory
covers all 80 admin operations. Live/offline documents are compared exactly, and a real CXF test interleaves
the admin application with two differently configured existing UpMan applications in one process.
It also checks that global model converters remain unchanged.

Maintain explanations in `rest-admin/src/main/resources/io/imunity/rest/admin/openapi/operations.properties`.
Adjacent `contract` fixtures are shared with the HTTP tests and remain independent of DTO serialization.
Admin timestamps, flattened invitations/request states, plugin payloads, error ordering and media types
retain their existing contracts. See `rest-admin/src/test/java/pl/edu/icm/unity/restadm/HTTP_CONTRACT_COVERAGE.md`.

Review semantic changes to paths, media types, statuses, required fields, enums, discriminators and security
when API code changes. Do not edit generated YAML or HTML. The former `-PopenapiSpec` profile is retained as
an alias; its fixed-port bootstrap and download step are replaced by the normal deterministic exporter.
No persisted models change and no database migration is required.
