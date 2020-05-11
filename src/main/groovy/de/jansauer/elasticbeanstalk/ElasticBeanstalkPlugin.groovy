package de.jansauer.elasticbeanstalk

import de.jansauer.elasticbeanstalk.tasks.CleanupVersionsTask

import de.jansauer.elasticbeanstalk.tasks.CreateVersionTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class ElasticBeanstalkPlugin implements Plugin<Project> {

  void apply(Project project) {
    def extension = project.extensions.create('elasticBeanstalk', ElasticBeanstalkExtension, project)

    project.tasks.register("CreateVersion", CreateVersionTask) {
      description = 'Creates a AWS Elastic Beanstalk application version'
      group = 'aws'

      applicationRegion = extension.region
      applicationName = extension.application
      versionBucket = extension.bucket
      versionLabel = extension.versionLabel
        .orElse(project.version)
      versionDescription = extension.versionDescription
    }

    project.tasks.register('CleanupVersions', CleanupVersionsTask) {
      description = 'Deletes excessive AWS Elastic Beanstalk application versions'
      group = 'aws'

      applicationRegion = extension.region
      applicationName = extension.application
      versionsToPreserve = extension.versionsToPreserve
    }
  }
}
