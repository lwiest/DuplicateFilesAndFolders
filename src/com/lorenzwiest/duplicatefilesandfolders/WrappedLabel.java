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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class WrappedLabel {
	public enum Type {
		WARNING,
		INFO
	}

	final private static Class<WrappedLabel> CLAZZ = WrappedLabel.class;
	final private static Image IMG_WARNING = ImageDescriptor.createFromFile(CLAZZ, "icons/warning.png").createImage();
	final private static Image IMG_INFO = ImageDescriptor.createFromFile(CLAZZ, "icons/info.png").createImage();

	private Composite composite;
	private Label icon;
	private Label text;

	public WrappedLabel(Composite parent) {
		this.composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).margins(0, 0).applyTo(this.composite);

		this.icon = new Label(this.composite, SWT.NONE);
		GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.TOP).applyTo(this.icon);

		this.text = new Label(this.composite, SWT.WRAP);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(this.text);
	}

	public void setMessage(String message, Type type) {
		if ((message == null) || message.isEmpty()) {
			this.text.setText("");
			this.icon.setImage(null);
		} else {
			this.text.setText(message);
			if (type == Type.WARNING) {
				this.icon.setImage(IMG_WARNING);
			} else if (type == Type.INFO) {
				this.icon.setImage(IMG_INFO);
			}
		}
		this.composite.layout(true);
	}

	public Control getControl() {
		return this.composite;
	}
}
