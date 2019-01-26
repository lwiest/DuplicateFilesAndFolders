# DuplicateFilesAndFolders

DuplicateFilesAndFolders is an open-source tool to find and (optionally) delete duplicate files and folders, written in Java.

It identifies duplicate files and folders by calculating and comparing MD5 hashes (something that Git does, too). For easy operation, DuplicateFilesAndFolders offers an Eclipse SWT/JFace-based graphical user interface.

I developed DuplicateFilesAndFolders on Windows 7 (64-bit), but it should be easy to port it to other platforms.

The DuplicateFilesAndFolders source code is available under the MIT license.

Enjoy -- Lorenz

## Table of Contents

* [Quick Start](#quick-start)
* [Usage](#usage)
* [Build Instructions](#build-instructions)
* [Porting Tips](#porting-tips)

## Quick Start

I have prepared a runnable JAR file for the Windows (64-bit) platform for you.

**Prerequisites:**
* You are running a Windows (64-bit) system.
* You have a Java SDK 7 (or higher) (64-bit) installed on your system.

1. Download this project's ZIP file from GitHub.
2. Extract the file `DuplicateFilesAndFolders.jar` from the ZIP file.
2. Double-click this file to run DuplicateFilesAndFolders.

## Usage
<img src="pics/image1.png" width="660"/>

1. Enter or choose a folder to scan.
2. Click _Find Duplicates_ to scan the folder.
3. Inspect the list of duplicate folders.
4. Inspect the list of duplicate files.
5. Duplicate items are grouped by background color.
6. Select a duplicate item for deletion.
7. Child items of selected items are automatically selected and dimmed.
8. (Optional) Select all items, select all but one item of each duplicate item, or deselect all items for deletion.
9. (Optional) Open a folder (or the containing folder of a file) with the context menu.
10. Open the _Confirm Deleting Items_ dialog.

<img src="pics/image2.png" width="660"/>

1. Confirm to delete the selected items.
2. Delete the selected items.

:exclamation: **Important: Like with all software able to delete files you are using DuplicateFilesAndFolders at your own risk.** :exclamation:

## Build Instructions

**Prerequisites:**
* You are running a Windows (64-bit) system.
* You have a Java SDK installed on your system (I used Java SDK 8 (64-bit)).
* You have an Eclipse IDE installed on your system (I used Eclipse 4.5.0 "Mars" (64-bit)).

1. Download the ZIP file of this project from GitHub.
2. Unzip it to a temporary folder ("temp folder").
3. Import the `DuplicateFilesAndFolders` project from the temp folder to your Eclipse IDE as an import source _General > Existing Projects into Workspace_.
4. Add the required Eclipse libraries:
	1. In the _Project Explorer_ view, right-click on the _DuplicateFilesAndFolders_ project and select _Build Path > Configure Build Path..._
	2. Click tab _Libraries_.
	3. Remove any JARs with an error marker.
	4. Click the _Add External JARs..._ button and add the following libraries from your Eclipse IDE's plugins folder (`XXX` is a placeholder for a version string):
		* `org.eclipse.core.commands_XXX.jar`
		* `org.eclipse.equinox.common_XXX.jar`
		* `org.eclipse.jface_XXX.jar`
		* `org.eclipse.swt.win32.win32.x86_64_XXX.jar`
		* `org.eclipse.swt_XXX.jar`
	5. Close the dialog with _OK_.
	6. There should be no more error markers in the _Project Explorer_ view.
5. In the _Project Explorer_ view, right-click _DuplicateFilesAndFolders_ and select _Run As > Java Application_.
6. The DuplicateFilesAndFolders application starts. Close it.
7. In the _Project Explorer_ view, right-click _DuplicateFileAndFolders_ and select _Export..._.
8. Select _Java > Runnable JAR file_.
9. Click _Next >_.
10. Under _Launch Configuration_, select _Duplicate FilesAndFolders - Duplicate FilesAndFolders_.
11. Under _Export destination_, enter the full pathname of the exported application, for example `C:\TEMP\DuplicateFilesAndFolders.jar`.
12. Select the radio button _Package required libraries into generated JAR_.
13. Click _Finish_.
14. Use a file explorer to find the exported JAR file.
15. To run the exported JAR file, double-click it.

## Porting Tips

To port DuplicateFilesAndFolders to another platform, apply the following changes:

1. Adjust in method `Utils.getInitialFolderToScanPath()` the string that is shown as the initial folder path.
2. Adjust in method `Utils.getExplorerCommandLine()` the command-line string that launches the file explorer, opening a specific folder.
3. Replace the SWT library `org.eclipse.swt.win32.win32.x86_64_XXX.jar` with the SWT library specific to your platform (Tip: The library name follows the pattern `org.eclipse.swt.<platform>_<version>.v<timestamp>.jar`.).
