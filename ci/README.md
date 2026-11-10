# Native Android scheduling

`native-android.Jenkinsfile` selects an online Mac whose device resource is free, preferring mini-02. It acquires the resource before starting the matching Freestyle worker, so queued builds do not occupy Mac executors. A started build is never retried on another Mac after a test, build or publishing failure.

| Agent | Shared device resource | Worker |
| --- | --- | --- |
| ci-zg-mac-mini-02 | mmt-mac-mini-02-devices | native-android-workers/mac-mini-02 |
| ci-zg-mac-mini-03 | mmt-mac-mini-03-devices | native-android-workers/mac-mini-03 |

The device resources are also used by native iOS, Huawei, plugin E2E and cache maintenance. Both workers use their host's `android37` AVD: Pixel 10, ARM64, API 37.1 Google Play 16 KB image revision 8. Matching AVD names on different hosts refer to separate devices. Workers retain the scoped emulator cleanup, which stops only an emulator started by that build and leaves reused emulators running.

The dispatcher additionally holds `mmt-native-android-build`. The release job requests this resource together with `mmt-mac-mini-02-devices`, preventing snapshot and release runs from publishing concurrently. Do not remove either release resource or acquire the global resource after a device resource.

Normal builds retain `BRANCH_NAME_TO_BUILD` and `WITH_SQ_REPORT`. `MAC_AGENT` supports `auto` or either explicit Mac; `DONT_PUSH_ON_GITHUB` suppresses source mirroring, and `SCHEDULER_CHECK_ONLY` checks allocation without building or changing devices. Native dispatcher concurrency is disabled without aborting an existing run. Workers retain the original 45-minute build timeout; the dispatcher separately bounds executor-start waiting.

If a worker cannot start within that bound, Jenkins may report either a timeout interruption or an `AbortException` indicating that the queue request was cancelled. Both permit trying another Mac only before a worker run has been returned. Other errors and explicit Pipeline interruptions propagate; started builds are never retried. Cancelling only a queued child request does not stop the dispatcher: stop the parent Pipeline to cancel the overall build.

Workers preserve Jenkins-managed credentials, Gradle tasks, SonarQube, signing support files and mirror branch eligibility. Each has a separate workspace ending in `infobip-mobile-messaging-android`. Direct manual starts are blocked, allowed upstream jobs are restricted to the dispatcher, and the first shell verifies the parent, node and device resource. The dispatcher owns the locks through worker completion and report collection; workers must not reacquire them.

Worker artifacts include the tested SDK commit and allocation in `ci-artifacts/dispatch.json`, JUnit XML and emulator diagnostics. The entry-point build copies those artifacts, publishes JUnit results, links the worker console, and reports the tested SDK commit to Bitbucket. First-failure and recovery notifications use the existing `mmt_mobile_jenkins_failures` Slack channel.

## Releases and push hooks

The normal job URL is `Team_Mobile_Messaging/infobip-mobile-messaging-android`. The existing Freestyle Release form and old build history are retained under `infobip-mobile-messaging-android-release`, on mini-02. Use that job's **Release** action for SDK releases. The dispatcher performs snapshot/test builds and optional source mirroring; it does not publish an SDK release. Release execution was not exercised as part of the migration.

The existing Infobip `/gitlab/build_now` hook discovers Freestyle/Maven jobs rather than Pipelines. `native-android-workers/push-trigger` advertises the literal repository URL, forwards build parameters to the dispatcher and exits without waiting. It must not contain SDK work, device locks or release credentials. Workers and the release job use trusted environment variables for SCM URLs and ignore commit notifications, preventing duplicate automatic builds.

## Rollout and rollback

`native-android-workers.groovy` is the worker/relay provisioning blueprint. Run it with sandboxed Job DSL and a configured run identity, or apply the same settings through normal Jenkins configuration. Do not disable the sandbox to bypass a provisioning failure. The template copies Jenkins-managed credentials without exporting them into Git.

1. Validate a copied snapshot job on mini-03 under its device lock, with source mirroring and release actions disabled. Do not interrupt running jobs.
2. Provision workers and a candidate dispatcher; validate scheduling, worker admission, tests and report copying. Keep the existing job available while doing this.
3. Drain the existing Android job, preserving queued requests and build history. Move it to the release name, isolate it from push discovery and retain its Release wrapper, parameters and credentials. Add the global native Android resource to its existing device resource.
4. Move the validated dispatcher to the original job name, and enable the nonblocking push relay. Use the source-controlled definition from the chosen published branch; record the deployed ref in the rollout report.

For rollback, disable the relay and wait for dispatcher, worker and queued runs to finish. Rename the dispatcher out of the original name and restore the release job to that name, its literal SCM URLs and original commit-notification behavior. Preserve all histories. Do not stop other jobs or force-unlock resources.
