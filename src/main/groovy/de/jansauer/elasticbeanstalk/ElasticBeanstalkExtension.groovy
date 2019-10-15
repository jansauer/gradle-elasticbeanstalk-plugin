package de.jansauer.elasticbeanstalk

import org.gradle.api.Project
import org.gradle.api.provider.Property

import java.util.regex.Pattern

class ElasticBeanstalkExtension {

  final Property<String> applicationName
  final Property<Integer> versionToPreserve
  final Property<Pattern> versionToRemoveRegex

  ElasticBeanstalkExtension(Project project) {
    applicationName = project.objects.property(String)

    versionToRemoveRegex = project.objects.property(String)
    versionToRemoveRegex.convention(/\d+\.\d+\.\d+-\d+-g.*/)

    versionToPreserve = project.objects.property(Integer)
    versionToPreserve.set(8)
  }
}
