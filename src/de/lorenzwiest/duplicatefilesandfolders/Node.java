/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Lorenz Wiest
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package de.lorenzwiest.duplicatefilesandfolders;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Node {
	final public static List<Node> EMPTY = Collections.emptyList();

	private File file;
	private List<Node> children;
	private String strHash;
	private long size;
	private int numTotalChildren;

	public Node(File file) {
		this.file = file;
		this.children = null;
	}

	public File getFile() {
		return this.file;
	}

	public void addChild(Node child) {
		if (this.children == null) {
			this.children = new ArrayList<Node>();
		}
		this.children.add(child);
	}

	public List<Node> getChildren() {
		return (this.children != null) ? this.children : EMPTY;
	}

	public void setHash(String strHash) {
		this.strHash = strHash;
	}

	public String getHash() {
		return this.strHash;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public long getSize() {
		return this.size;
	}

	public void setNumTotalChildren(int numTotalChildren) {
		this.numTotalChildren = numTotalChildren;
	}

	public int getTotalChildrenCount() {
		return this.numTotalChildren;
	}

	@Override
	public String toString() {
		return this.file.getAbsolutePath();
	}
}