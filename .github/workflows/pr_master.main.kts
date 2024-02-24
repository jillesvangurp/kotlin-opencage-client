#!/usr/bin/env kotlin

@file:DependsOn("io.github.typesafegithub:github-workflows-kt:1.8.0")

import io.github.typesafegithub.workflows.actions.actions.*
import io.github.typesafegithub.workflows.actions.docker.BuildPushActionV5
import io.github.typesafegithub.workflows.actions.docker.SetupBuildxActionV3
import io.github.typesafegithub.workflows.actions.googlegithubactions.AuthV2
import io.github.typesafegithub.workflows.actions.googlegithubactions.SetupGcloudV1
import io.github.typesafegithub.workflows.actions.googlegithubactions.SetupGcloudV2
import io.github.typesafegithub.workflows.actions.gradle.GradleBuildActionV2
import io.github.typesafegithub.workflows.domain.RunnerType
import io.github.typesafegithub.workflows.domain.triggers.PullRequest
import io.github.typesafegithub.workflows.domain.triggers.Push
import io.github.typesafegithub.workflows.dsl.expressions.expr
import io.github.typesafegithub.workflows.dsl.workflow
import io.github.typesafegithub.workflows.yaml.writeToFile

val workflow = workflow(
    name = "Process Pull Request",
    on = listOf(
        Push(
            branches = listOf("main")
        ),
        PullRequest(
            branches = listOf("main")
        ),
    ),
    sourceFile = __FILE__.toPath(),
    targetFileName = "pr_master.yaml",
) {
    job(
        id = "build-and-test",
        name = "Build And Test",
        runsOn = RunnerType.UbuntuLatest,
        timeoutMinutes = 30,
    ) {
        uses(
            name = "checkout",
            action = CheckoutV4()
        )
        uses(
            name = "setup jdk",
            action = SetupJavaV4(
                javaPackage = SetupJavaV4.JavaPackage.Jdk,
                javaVersion = "21",
                distribution = SetupJavaV4.Distribution.Corretto,
                cache = SetupJavaV4.BuildPlatform.Gradle,
            )
        )
        uses(
            name = "cache konan dir",
            action = CacheV3(
                path = listOf("~/.konan/**/*"),
                key = ".konan"
            )
        )
        run {
            val gradleBuildStep = uses(
                name = "build with gradle",
                action = GradleBuildActionV2(
                    arguments = "check",
                ),
                env = linkedMapOf(
                    "OPENCAGE_KEY" to expr("secrets.OPENCAGE_KEY")
                ),
            )
        }

    }
}

workflow.writeToFile(addConsistencyCheck = true)
