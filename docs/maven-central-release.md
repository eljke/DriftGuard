# Maven Central Release

DriftGuard is prepared for publication through Sonatype Central Portal.

## One-time setup

1. Create an account at <https://central.sonatype.com/>.
2. Register and verify the namespace `io.github.eljke`.
3. Generate a Central Portal user token.
4. Generate a GPG key for artifact signing.
5. Add these GitHub repository secrets:
   - `CENTRAL_USERNAME`
   - `CENTRAL_PASSWORD`
   - `GPG_PRIVATE_KEY`
   - `GPG_PASSPHRASE`

## Release

1. Update `<revision>` in the root `pom.xml`.
2. Create and push a release tag, for example `v1.0.0`.
3. Publish a GitHub Release from the tag, or run the `Publish Maven Central` workflow manually.

The workflow runs:

```bash
./mvnw -B -Prelease deploy
```

Maven Central versions are immutable. If `1.0.0` is published once, fixes must use a new version such as `1.0.1`.
