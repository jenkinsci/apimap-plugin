package io.apimap;

import io.apimap.plugin.jenkins.utils.RestClientUtil;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UrlRestrictionsTest {
    @Test
    public void urlWithArguments_didFail() {
        assertFalse(RestClientUtil.bareboneURL("https://www.google.com/search?channel=fs&client=ubuntu"));
    }

    @Test
    public void urlWithoutArguments_didSucceed() {
        assertTrue(RestClientUtil.bareboneURL("https://api.apimap.io/"));
    }
}
