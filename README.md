# AndroidOMRChecker
An android application for validating images of OMR sheets before they are sent for processing.

## Credits
Note: This repository uses [androidomr](https://github.com/udayraj123/androidomr) as a foundation code. Thus substantial credits to [udayraj123](https://github.com/udayraj123).

Hits Since **8 Apr '19**: [![HitCount](http://hits.dwyl.io/Udayraj123/AndroidOMRChecker.svg)](http://hits.dwyl.io/Udayraj123/AndroidOMRChecker)

# androidomr

androidomr is an Android document detection library built on top of OpenCV. It scans documents from camera live mode and allows you to adjust crop using the detected 4 edges and performs perspective transformation of the cropped image.

**It works best with a dark background.**

# JavaDocs
You can browse the [JavaDocs for the latest release](https://udayraj123.github.io/androidomr/docs)

# Integrating into your project
This library is available in [JitPack.io](https://jitpack.io/) repository.
To use it, make sure to add the below inside root build.gradle file

```
allprojects {
    repositories {
        mavenCentral()
        maven { url "https://jitpack.io" }
    }
}
```

and add the repository's url to the app's build.gradle file.

```
dependencies {
   compile 'com.github.udayraj123:androidomr:1.0.6'

    // Other dependencies your app might use
}
```
# Usage
Out of the box it uses OpenCV.

1. Start **startActivityForResult** from your activity
```
startActivityForResult(new Intent(this, ScanActivity.class), REQUEST_CODE);
```
2. Get a file path for cropped image on **onActivityResult**
```
String filePath = data.getExtras().getString(ScanConstants.SCANNED_RESULT);
Bitmap baseBitmap = ScanUtils.decodeBitmapFromFile(filePath, ScanConstants.IMAGE_NAME);
```
3. Display the image using **TouchImageView**
```
<com.udayraj.androidomr.view.TouchImageView
        android:id="@+id/scanned_image"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:scaleType="center" />
```

# Help
**Versioning policy**

We use [Semantic Versioning 2.0.0](https://semver.org/) as our versioning policy.

**Bugs, Feature requests**

Found a bug? Something that's missing? Feedback is an important part of improving the project, so please [open an issue](https://github.com/udayraj123/androidomr/issues).

**Code**

Fork this project and start working on your own feature branch. When you're done, send a Pull Request to have your suggested changes merged into the master branch by the project's collaborators. Read more about the [GitHub flow](https://guides.github.com/introduction/flow/).
