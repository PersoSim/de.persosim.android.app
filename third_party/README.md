# PersoSim source dependencies

The Java sources here are compiled by `:simulator` on every source build.
[sources.json](sources.json) pins each upstream repository, commit and source
subdirectory. No binaries were copied from the Android APK.

- PersoSim simulator, driver connector and RemoteIFD: June 2021 release, simulator
  version 0.17.3, close to the original Android 0.17.2 runtime.
- GlobalTester logging, simulator interfaces, crypto provider and XStream adapter:
  matching 3.8.3 release sources.
- Only Java sources and module license texts are vendored. Desktop Eclipse
  preference/device adapters and unused OSGi entrypoints are excluded in
  [simulator/build.gradle](../simulator/build.gradle).

Local changes:

- `DateAuxObject` and `Utils`: use the original profiles' Berlin date convention
  for auxiliary date comparisons and updates, independent of the device timezone.

- `PersonalizationFactory`: explicit UTF-8 and type permissions for XStream 1.4.21.
- `Serializer`: permissions for internal TLV/state copies, explicit SecretKeySpec
  conversion without private JDK reflection, synchronized class-loader updates.
- `PairingServer` and `DefaultHandshaker`: Bouncy Castle 1.87 TLS hooks, TLS 1.2
  for the existing RemoteIFD protocol and client-certificate pairing.
- `WebsocketComm`: close sockets reliably, stop/restart without self-joining,
  bound stalled client reads and expose the bound port for integration tests.
- `WebSocketProtocol` and `DefaultMessageHandler`: process commands in order,
  cancel pending commands before releasing a connection, preserve bytes after
  the HTTP handshake and serialize complete response frames of bounded size.

Original attribution and license files are retained in the module directories.
The upstream RemoteIFD module has no separate LICENSE file at the pinned commit;
its provenance is retained in sources.json and its authors in source headers.
The application repository's GPLv3 license remains in [LICENSE](../LICENSE).

External dependencies (resolved by Gradle, not vendored): XStream 1.4.21 (BSD),
Spongy Castle 1.58.0.0 (Bouncy Castle license), Bouncy Castle 1.87 (Bouncy Castle
license), OSGi Core 6.0.0 (Apache-2.0), Android desugaring library 2.1.5
(GPLv2 with Classpath Exception), JUnit 4.13.2 (EPL-1.0), and JSON-java 20210307
(test/compile dependency only; Android supplies org.json at runtime).
