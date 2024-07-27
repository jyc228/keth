import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.maven

fun RepositoryHandler.gpr(path: String, action: MavenArtifactRepository.() -> Unit = {}) {
    maven("https://maven.pkg.github.com/$path") {
        action(this)
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

fun Project.createGPRPublisher(configuration: MavenPublication.() -> Unit): PublishingExtension.() -> Unit {
    return {
        repositories {
            gpr("jyc228/keth") {
                name = "GitHubPackages"
            }
        }
        publications {
            create<MavenPublication>("gpr") {
                this.from(components["java"])
                this.groupId = "io.github.jyc228.keth"
                this.version = project.version.toString()
                this.configuration()
            }
        }
    }
}
