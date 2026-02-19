package com.rafgittools.ui.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.net.URLDecoder

class ScreenRouteEncodingTest {

    private val owner = "org name/with#chars%and?query"
    private val repo = "repo name/with#chars%and?query"
    private val username = "user name/with#chars%and?query"

    @Test
    fun issueListRoute_encodesAndDecodesArguments() {
        val route = Screen.IssueList.createRoute(owner, repo)
        val segments = route.removePrefix("issue_list/").split("/")

        assertThat(segments).hasSize(2)
        assertThat(URLDecoder.decode(segments[0], "UTF-8")).isEqualTo(owner)
        assertThat(URLDecoder.decode(segments[1], "UTF-8")).isEqualTo(repo)
    }

    @Test
    fun issueDetailRoute_encodesAndDecodesArguments() {
        val route = Screen.IssueDetail.createRoute(owner, repo, 42)
        val segments = route.removePrefix("issue_detail/").split("/")

        assertThat(segments).hasSize(3)
        assertThat(URLDecoder.decode(segments[0], "UTF-8")).isEqualTo(owner)
        assertThat(URLDecoder.decode(segments[1], "UTF-8")).isEqualTo(repo)
        assertThat(segments[2]).isEqualTo("42")
    }

    @Test
    fun pullRequestRoutes_encodeAndDecodeArguments() {
        val listRoute = Screen.PullRequestList.createRoute(owner, repo)
        val listSegments = listRoute.removePrefix("pr_list/").split("/")
        assertThat(URLDecoder.decode(listSegments[0], "UTF-8")).isEqualTo(owner)
        assertThat(URLDecoder.decode(listSegments[1], "UTF-8")).isEqualTo(repo)

        val detailRoute = Screen.PullRequestDetail.createRoute(owner, repo, 7)
        val detailSegments = detailRoute.removePrefix("pr_detail/").split("/")
        assertThat(URLDecoder.decode(detailSegments[0], "UTF-8")).isEqualTo(owner)
        assertThat(URLDecoder.decode(detailSegments[1], "UTF-8")).isEqualTo(repo)
        assertThat(detailSegments[2]).isEqualTo("7")
    }

    @Test
    fun profileRoute_encodesAndDecodesUsername() {
        val route = Screen.Profile.createRoute(username)
        val encodedUsername = route.removePrefix("profile/")

        assertThat(URLDecoder.decode(encodedUsername, "UTF-8")).isEqualTo(username)
    }

    @Test
    fun releaseRoutes_encodeAndDecodeArguments() {
        val listRoute = Screen.Releases.createRoute(owner, repo)
        val listSegments = listRoute.removePrefix("releases/").split("/")
        assertThat(URLDecoder.decode(listSegments[0], "UTF-8")).isEqualTo(owner)
        assertThat(URLDecoder.decode(listSegments[1], "UTF-8")).isEqualTo(repo)

        val detailRoute = Screen.ReleaseDetail.createRoute(owner, repo, 99L)
        val detailSegments = detailRoute.removePrefix("release_detail/").split("/")
        assertThat(URLDecoder.decode(detailSegments[0], "UTF-8")).isEqualTo(owner)
        assertThat(URLDecoder.decode(detailSegments[1], "UTF-8")).isEqualTo(repo)
        assertThat(detailSegments[2]).isEqualTo("99")
    }

    @Test
    fun createRoutes_encodeAndDecodeArguments() {
        val issueRoute = Screen.CreateIssue.createRoute(owner, repo)
        val issueSegments = issueRoute.removePrefix("create_issue/").split("/")
        assertThat(URLDecoder.decode(issueSegments[0], "UTF-8")).isEqualTo(owner)
        assertThat(URLDecoder.decode(issueSegments[1], "UTF-8")).isEqualTo(repo)

        val prRoute = Screen.CreatePullRequest.createRoute(owner, repo)
        val prSegments = prRoute.removePrefix("create_pr/").split("/")
        assertThat(URLDecoder.decode(prSegments[0], "UTF-8")).isEqualTo(owner)
        assertThat(URLDecoder.decode(prSegments[1], "UTF-8")).isEqualTo(repo)
    }
}
