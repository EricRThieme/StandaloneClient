package org.stt.connector.jira

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.stt.config.JiraConfig

internal class JiraClientTest {

  @Test
  @Throws(AccessDeniedException::class, InvalidCredentialsException::class, IssueDoesNotExistException::class)
  fun testErrorHandlingOfIssueRequest() {
    val jiraConfig = JiraConfig()
    jiraConfig.jiraURI = "https://jira.atlassian.net"
    assertThatThrownBy {
      JiraClient("dummy", null, jiraConfig.jiraURI!!).getIssue("JRA-7")
    }.isInstanceOf(IssueDoesNotExistException::class.java)
      .hasMessageContainingAll("Couldn't find issue JRA-7", "\"errorMessage\": \"Site temporarily unavailable\"")
  }
}