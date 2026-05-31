package org.stt.connector.jira

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.stt.config.JiraConfig
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

internal class JiraClientTest {

  @Test
  @Throws(AccessDeniedException::class, InvalidCredentialsException::class, IssueDoesNotExistException::class)
  fun testErrorHandlingOfIssueRequest() {
    val jiraConfig = JiraConfig()
    jiraConfig.jiraURI = "https://jira.atlassian.net"

    @Suppress("UNCHECKED_CAST")
    val mockResponse = mock(HttpResponse::class.java) as HttpResponse<String>
    `when`(mockResponse.statusCode()).thenReturn(404)
    `when`(mockResponse.body()).thenReturn("""{"errorMessage": "Site temporarily unavailable"}""")

    val mockHttpClient = mock(HttpClient::class.java)
    @Suppress("UNCHECKED_CAST")
    `when`(mockHttpClient.send(
      org.mockito.ArgumentMatchers.any(HttpRequest::class.java),
      org.mockito.ArgumentMatchers.any<HttpResponse.BodyHandler<String>>()
    )).thenReturn(mockResponse)

    assertThatThrownBy {
      JiraClient("dummy", null, jiraConfig.jiraURI!!, mockHttpClient).getIssue("JRA-7")
    }.isInstanceOf(IssueDoesNotExistException::class.java)
      .hasMessageContainingAll("Couldn't find issue JRA-7", "\"errorMessage\": \"Site temporarily unavailable\"")
  }
}