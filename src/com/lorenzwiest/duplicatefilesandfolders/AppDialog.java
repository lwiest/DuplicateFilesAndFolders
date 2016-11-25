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

package com.lorenzwiest.duplicatefilesandfolders;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;

public class AppDialog extends Dialog {
	private final Point INITIAL_SIZE = new Point(SWT.DEFAULT, SWT.DEFAULT);

	protected AppDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	protected Point getPreferredSize() {
		return this.INITIAL_SIZE;
	}

	@Override
	protected Point getInitialSize() {
		Point size = super.getInitialSize();
		Point preferredSize = getPreferredSize();
		size.x = (preferredSize.x == SWT.DEFAULT) ? size.x : preferredSize.x;
		size.y = (preferredSize.y == SWT.DEFAULT) ? size.y : preferredSize.y;
		return size;
	}

	@Override
	protected Point getInitialLocation(Point initialSize) {
		Point location = super.getInitialLocation(initialSize);
		Rectangle parentRect = getShell().getParent().getBounds();
		location.x = parentRect.x + ((parentRect.width - initialSize.x) / 2);
		location.y = parentRect.y + ((parentRect.height - initialSize.y) / 2);
		return location;
	}
}
