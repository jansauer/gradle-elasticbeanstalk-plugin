package de.jansauer.elasticbeanstalk

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ElasticBeanstalkExtensionTest extends Specification {

  def "should have some extension defaults"() {
    when:
    Project project = ProjectBuilder.builder().withName("hello-world").build()
    project.pluginManager.apply ElasticBeanstalkPlugin

    then:
    project.extensions.elasticBeanstalk instanceof ElasticBeanstalkExtension
    project.extensions.elasticBeanstalk.versionDescription.get() == ''
    project.extensions.elasticBeanstalk.versionsToPreserve.get() == 30
  }
}
