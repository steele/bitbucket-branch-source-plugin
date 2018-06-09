/*
 * The MIT License
 *
 * Copyright (c) 2016-2017, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.cloudbees.jenkins.plugins.bitbucket.filesystem;

import com.cloudbees.jenkins.plugins.bitbucket.PullRequestSCMHead;
import com.cloudbees.jenkins.plugins.bitbucket.api.BitbucketApi;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.IOException;
import java.io.InputStream;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;

public class BitbucketSCMFile  extends SCMFile {

	private final BitbucketApi api;
	private  String ref;
	private final String hash;
	private BitbucketSCMFileSystem fileSystem;
	
	public String getRef() throws IOException, InterruptedException {
		if (ref.matches("PR-\\d+")) { // because JENKINS-48737
			return getRefForPullRequest();
		} else {
			return ref;
		}
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	@Deprecated
	public BitbucketSCMFile(BitbucketSCMFileSystem bitBucketSCMFileSystem,
							BitbucketApi api,
							String ref) {
		this(bitBucketSCMFileSystem, api, ref, null);
	}

	public BitbucketSCMFile(BitbucketSCMFileSystem bitBucketSCMFileSystem,
							BitbucketApi api,
							String ref, String hash) {
		super();
		type(Type.DIRECTORY);
		this.api = api;
		this.ref = ref;
		this.hash = hash;
		this.fileSystem = bitBucketSCMFileSystem;
	}

	@Deprecated
	public BitbucketSCMFile(@NonNull BitbucketSCMFile parent, String name, Type type) {
		this(parent, name, type, null);
	}

	public BitbucketSCMFile(@NonNull BitbucketSCMFile parent, String name, Type type, String hash) {
    	super(parent, name);
    	this.api = parent.api;
    	this.ref = parent.ref;
    	this.hash = hash;
    	type(type);
    }

	public String getHash() {
		return hash;
	}

	@Override
	@NonNull
	public Iterable<SCMFile> children() throws IOException,
			InterruptedException {
        if (this.isDirectory()) {
            return api.getDirectoryContent(this);
        } else {
            throw new IOException("Cannot get children from a regular file");
        }
	}

	@Override
	@NonNull
	public InputStream content() throws IOException, InterruptedException {
        if (this.isDirectory()) {
            throw new IOException("Cannot get raw content from a directory");
        } else {
        	return api.getFileContent(this);
        }
	}

	@Override
	public long lastModified() throws IOException, InterruptedException {
		// TODO: Return valid value when Tag support is implemented
		return 0;
	}

	@Override
	@NonNull
	protected SCMFile newChild(String name, boolean assumeIsDirectory) {
		return new BitbucketSCMFile(this, name, assumeIsDirectory?Type.DIRECTORY:Type.REGULAR_FILE, hash);

	}

	@Override
	@NonNull
	protected Type type() throws IOException, InterruptedException {
		return this.getType();
	}

	/**
	 * Get the correct reference of a pull request with respect to the checkout
	 * strategy.
	 *
	 * This is related to JENKINS-48737. The content of file.getRef() is either the
	 * branch name or in case of a pull request it is something like "PR-123". Since
	 * there is no reference named like this in bitbucket it has to be changed to
	 * the correct name. Also the configured checkout strategy for the job has to be
	 * taken into account.
	 *
	 * @return the pull request reference
	 */
	private String getRefForPullRequest() throws IOException, InterruptedException {
		ChangeRequestCheckoutStrategy strategy = getCheckoutStrategy();
		String prId = ref.replace("PR-", "");
		String source = "";
		if (strategy != null) {
			if (strategy == ChangeRequestCheckoutStrategy.MERGE) {
				source = "merge";
			} else {
				source = "from";
			}
		} else {
			throw new UnsupportedOperationException(
					"Can not get reference for this pull request since checkout strategy can not be determined");
		}
		return "refs/pull-requests/" + prId + "/" + source;
	}

	@CheckForNull
	private ChangeRequestCheckoutStrategy getCheckoutStrategy() throws IOException, InterruptedException {
		if (this.isDirectory()) {
			SCMRevision revision = fileSystem.getRevision();
			if (revision != null) {
				SCMHead head = revision.getHead();
				if (head instanceof PullRequestSCMHead) {
					return ((PullRequestSCMHead) head).getCheckoutStrategy();
				}
			}
		} else {
			SCMFile parent = this.parent();
			if (parent instanceof BitbucketSCMFile) {
				return ((BitbucketSCMFile) parent).getCheckoutStrategy();
			}
		}
		return null;
	}
}
