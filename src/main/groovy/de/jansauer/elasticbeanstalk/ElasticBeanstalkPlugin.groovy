package de.jansauer.elasticbeanstalk

import org.gradle.api.Plugin
import org.gradle.api.Project

class ElasticBeanstalkPlugin implements Plugin<Project> {

  void apply(Project target) {
    def extension = target.extensions.create('elasticBeanstalk', ElasticBeanstalkExtension, target)
    target.tasks.create('cleanupVersions', CleanupVersionsTask) {
      applicationName = extension.applicationName
      versionToPreserve = extension.versionToPreserve
      versionToRemoveRegex = extension.versionToRemoveRegex
    }
  }
}
