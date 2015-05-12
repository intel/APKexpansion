phonegap-xapkreader
===================

Cordova plugin to access files in APK Expansion Files for Cordova/Phonegap Android application.

The plugin is an implementation of the process described here : [APK Expansion Files](http://developer.android.com/google/play/expansion-files.html)

# Installation

This plugin use the Cordova CLI's plugin command. To install it to your application, simply execute the following (and replace variables).

```
cordova plugin add org.apache.cordova.xapkreader --variable MAIN_VERSION=1 --variable MAIN_FILESIZE=12345 --variable PATCH_VERSION=0 --variable PATCH_FILESIZE=0 --variable YOUR_PUBLICKEY="your own application publickkey" --variable DOWNLOAD_OPTION=true
```

- `MAIN_VERSION` :  The version of your main expansion file. Can be 1 or >1.
- `PATCH_VERSION` :  The version of your patch expansion file. Should be 0 if no patch exists.
- `MAIN_FILESIZE` : The byte size of your main expansion file. This is used to verify the intégrity of the file after downloading.
- `PATCH_FILESIZE` : The byte size of your patch expansion file. Should be 0, if no patch exists.
- `YOUR_PUBLICKEY` : Your own application public key.
- `DOWNLOAD_OPTION` : Can be `true` or `false`. Define if your APK expansion file is downloaded from the play store(true) or manually add it(false). The false option is mainly for testing purpose.

# Using

The file is returned to a success callback as URL object that you can use like in the example below or with the File API.

```
XAPKReader.get(filename, successCallback, [errorCallback], [fileType]);
```

## Parameters

- **filename** : The name of the file to load form the expansion file
- **successCallback** : The callback that executes when the file is loaded.
- **errorCallback** (Optional) : The callback that executes if an error occurs.
- **fileType** (Optional) : The file type.

## Example

```javascript
XAPKReader.get(
    'image.jpg',
    function (url) {
        var img = new Image();
        img.src = url;
        document.body.appendChild(img);
    },
    function (error) {
        console.error(error);
    },
    'image/jpeg'
);
```

# Licence MIT

Copyright (c) 2015 Intel Corporation

Copyright 2013 Quentin Aupetit

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.