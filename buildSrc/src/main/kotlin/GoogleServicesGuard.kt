import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult

/**
 * Dependency groups Ephyra must never ship: Google Play Services, Firebase, Play Core and the Cast stack.
 * The app runs on GMS-free builds, so any of these is a bug even when it arrives transitively.
 */
private val forbiddenGroups = listOf(
	"com.google.android.gms",
	"com.google.firebase",
	"com.google.android.play",
	"com.google.android.datatransport",
	"com.google.mlkit",
	"com.google.android.ump",
	"androidx.mediarouter",
)

fun isForbiddenGroup(group: String): Boolean = forbiddenGroups.any { group == it || group.startsWith("$it.") }

private fun forbiddenModules(root: ResolvedComponentResult): Set<String> {
	val found = sortedSetOf<String>()
	val seen = mutableSetOf<ResolvedComponentResult>()
	val queue = ArrayDeque(listOf(root))

	while (queue.isNotEmpty()) {
		val component = queue.removeFirst()
		if (!seen.add(component)) continue

		val id = component.id
		if (id is ModuleComponentIdentifier && isForbiddenGroup(id.group)) found += id.displayName

		component.dependencies
			.filterIsInstance<ResolvedDependencyResult>()
			.forEach { queue += it.selected }
	}

	return found
}

/**
 * Register `verifyNoGoogleServices`, which fails the build when a shipped runtime classpath contains a Google service SDK,
 * and run it before every build of this project.
 */
fun Project.verifyNoGoogleServices() {
	val verifyTask = tasks.register("verifyNoGoogleServices") {
		group = "verification"
		description = "Fails if a runtime classpath contains Google Play Services, Firebase, Play Core or Cast."

		val graphs = configurations
			.filter { it.isCanBeResolved && it.name.endsWith("RuntimeClasspath") && !it.name.contains("Test") }
			.associate { it.name to it.incoming.resolutionResult.rootComponent }

		doLast {
			val offenders = graphs.flatMap { (name, root) -> forbiddenModules(root.get()).map { "$name: $it" } }
			if (offenders.isNotEmpty()) {
				throw GradleException("Google service dependencies found:\n  " + offenders.joinToString("\n  "))
			}
		}
	}

	tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(verifyTask) }
}
