package com.cloudbees.jenkins.plugins.bitbucket.mock;

import java.io.IOException;

import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketApi;
import com.cloudbees.jenkins.plugins.bitbucket.filesystem.BitbucketSCMFileSystem;

import jenkins.scm.api.SCMRevision;

public class MockBitbucketSCMFileSystem extends BitbucketSCMFileSystem {

    public MockBitbucketSCMFileSystem(BitbucketApi api, String ref, SCMRevision rev) throws IOException {
        super(api, ref, rev);
    }
}