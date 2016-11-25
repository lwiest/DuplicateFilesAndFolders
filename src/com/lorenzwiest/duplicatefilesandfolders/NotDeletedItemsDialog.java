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

import java.util.LinkedHashSet;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class NotDeletedItemsDialog extends AppDialog {
	private final static int INDENT = DuplicateFilesAndFolders.INDENT;
	private final static int INDENT2 = DuplicateFilesAndFolders.INDENT * 2;
	private final static String CR = System.getProperty("line.separator");

	private LinkedHashSet<Node> itemsNotDeleted;

	public NotDeletedItemsDialog(Shell shell, LinkedHashSet<Node> itemsNotDeleted) {
		super(shell);
		this.itemsNotDeleted = itemsNotDeleted;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Not Deleted Items");
		shell.setImage(DuplicateFilesAndFolders.IMG_ICON);
	}

	@Override
	protected Point getPreferredSize() {
		return new Point(500, 400);
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).margins(INDENT2, INDENT2).spacing(INDENT2, INDENT).applyTo(composite);
		GridDataFactory.swtDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(composite);

		Label lblIcon = new Label(composite, SWT.NONE);
		GridDataFactory.swtDefaults().align(SWT.BEGINNING, SWT.TOP).applyTo(lblIcon);
		lblIcon.setImage(Display.getCurrent().getSystemImage(SWT.ICON_ERROR));

		createRightSide(composite);

		new Label(composite, SWT.NONE);

		createOkButton(composite);

		return parent;
	}

	private void createRightSide(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().margins(0, 0).spacing(INDENT2, INDENT2).applyTo(composite);
		GridDataFactory.swtDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(composite);

		Label lblText = new Label(composite, SWT.WRAP);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.BEGINNING, SWT.TOP).applyTo(lblText);
		lblText.setText("The following items were not deleted:");

		createScrolledComposite(composite);
	}

	private void createScrolledComposite(Composite parent) {
		final ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		GridDataFactory.swtDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(scrolledComposite);
		GridLayoutFactory.swtDefaults().margins(0, 0).applyTo(scrolledComposite);

		final Composite composite = new Composite(scrolledComposite, SWT.NONE);
		scrolledComposite.setContent(composite);
		scrolledComposite.setExpandVertical(true);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				scrolledComposite.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		GridDataFactory.swtDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(composite);
		GridLayoutFactory.swtDefaults().margins(0, 0).applyTo(composite);

		StyledText text = new StyledText(composite, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY);
		GridDataFactory.swtDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).applyTo(text);
		StringBuffer sb = new StringBuffer();
		for (Node node : this.itemsNotDeleted) {
			sb.append(node.getFile().getAbsolutePath());
			sb.append(CR);
		}
		text.setText(sb.toString());
	}

	private void createOkButton(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().margins(0, 0).extendedMargins(0, 0, INDENT, 0).spacing(INDENT2, SWT.DEFAULT).applyTo(composite);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(composite);

		Button btnOK = new Button(composite, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.END, SWT.CENTER).hint(Utils.getButtonSize(btnOK)).applyTo(btnOK);
		btnOK.setText("OK");
		btnOK.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				close();
			}
		});

		getShell().setDefaultButton(btnOK);
	}
}