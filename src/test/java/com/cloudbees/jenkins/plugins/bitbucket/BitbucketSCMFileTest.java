package com.cloudbees.jenkins.plugins.bitbucket;

import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import jenkins.scm.impl.mock.MockSCMRevision;
import com.cloudbees.jenkins.plugins.bitbucket.filesystem.BitbucketSCMFile;
import com.cloudbees.jenkins.plugins.bitbucket.filesystem.BitbucketSCMFileSystem;
import com.cloudbees.jenkins.plugins.bitbucket.mock.MockBitbucketSCMFileSystem;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests the class {@link BitbucketSCMFile}.
 *
 * @author Benjamin Fuchs
 */
public class BitbucketSCMFileTest {

    @Test
    public void testGetRefSimple() throws Exception {
        BitbucketSCMFile dir = new BitbucketSCMFile(null, null, "master", null);
        assertEquals("master", dir.getRef());
    }

    @Test
    public void testGetRefForPullRequestForMergeStrategy() throws Exception {
        PullRequestSCMHead head = new PullRequestSCMHead("foo", "", "", "", "", null, null,
                ChangeRequestCheckoutStrategy.MERGE);
        MockSCMRevision rev = new MockSCMRevision(head, null);
        BitbucketSCMFileSystem fileSystem = new MockBitbucketSCMFileSystem(null, null, (SCMRevision) rev);
        BitbucketSCMFile dir = new BitbucketSCMFile(fileSystem, null, "PR-123", null);
        assertEquals("refs/pull-requests/123/merge", dir.getRef());
    }

    @Test
    public void testGetRefForPullRequestForHeadStrategy() throws Exception {
        PullRequestSCMHead head = new PullRequestSCMHead("foo", "", "", "", "", null, null,
                ChangeRequestCheckoutStrategy.HEAD);
        MockSCMRevision rev = new MockSCMRevision(head, null);
        BitbucketSCMFileSystem fileSystem = new MockBitbucketSCMFileSystem(null, null, (SCMRevision) rev);
        BitbucketSCMFile dir = new BitbucketSCMFile(fileSystem, null, "PR-123", null);
        assertEquals("refs/pull-requests/123/from", dir.getRef());
    }

    @Test
    public void testGetRefForPullRequestWithFile() throws Exception {
        PullRequestSCMHead head = new PullRequestSCMHead("foo", "", "", "", "", null, null,
                ChangeRequestCheckoutStrategy.HEAD);
        MockSCMRevision rev = new MockSCMRevision(head, null);
        BitbucketSCMFileSystem fileSystem = new MockBitbucketSCMFileSystem(null, null, (SCMRevision) rev);
        BitbucketSCMFile dir = new BitbucketSCMFile(fileSystem, null, "PR-123", null);
        BitbucketSCMFile file = new BitbucketSCMFile(dir, "Jenkinsfile", SCMFile.Type.REGULAR_FILE, null);
        assertEquals("refs/pull-requests/123/from", file.getRef());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetRefForPullRequestFailsWithError() throws Exception {
        SCMHead head = new SCMHead("foo");
        MockSCMRevision rev = new MockSCMRevision(head, null);
        BitbucketSCMFileSystem fileSystem = new MockBitbucketSCMFileSystem(null, null, (SCMRevision) rev);
        BitbucketSCMFile dir = new BitbucketSCMFile(fileSystem, null, "PR-123", null);
        dir.getRef();
    }
}
