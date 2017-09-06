var exec = require("cordova/exec");
var getQueue = [];
var inProgress = 0;

module.exports = {

    /**
     * Check for file permissions on new SDK
     **/
    checkFilePermissions: function(successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "XAPKReader", "checkPermissions");
    },

    /**
     * Get a file immediately in expansion file
     *
     * @param filename              The file name
     * @param fileType              The file type (eg: "image/jpeg")
     * @param successCallback       The callback to be called when the file is found.
     *                                  successCallback()
     * @param errorCallback         The callback to be called if there is an error.
     *                                  errorCallback(int errorCode) - OPTIONAL
     **/
    getImmediate: function(filename, successCallback, errorCallback, fileType) {
        // only for android
        if (!navigator.userAgent.match(/Android/i)) {
            return successCallback(filename);
        }

        if (null === filename) {
            console.error("XAPKReader.get failure: filename parameter needed");
            return;
        }

        var context = this;

        var success = function (result) {
            try {
                var url = context.arrayBufferToURL(result, fileType);
                successCallback(url);
            }
            catch (e) {
                errorCallback(e);
            }
        };

        cordova.exec(success, errorCallback, "XAPKReader", "get", [filename]);
    },

    /**
     * Adds queue to retrieve at most 10 get’s simultaneously.
     **/
    processQueue:  function() {
        while (inProgress < 10) {
            var e = getQueue.pop();
            if (!e) break;
            inProgress = inProgress + 1;
            this.getImmediate(e.filename, e.successCallback, e.errorCallBack, e.fileType);
        }
    },

    /**
     * Get a file in expansion file and return it as data base64 encoded
     *
     * @param filename              The file name
     * @param fileType              The file type (eg: "image/jpeg")
     * @param successCallback       The callback to be called when the file is found.
     *                                  successCallback()
     * @param errorCallback         The callback to be called if there is an error.
     *                                  errorCallback(int errorCode) - OPTIONAL
     **/
    get: function(filename, successCallback, errorCallBack, fileType) {
        var self = this;

        getQueue.push({filename: filename,
        successCallback: function (x) {
            successCallback(x);
            self.getFinished();
        },
        errorCallBack: function(x) {
            errorCallBack(x);
            self.getFinished();
        },
        fileType: fileType});

        this.processQueue();
    },

    /**
     * Progress queue termination of 10 gets
     **/
    getFinished: function() {
        console.log('getfinished');
        inProgress = inProgress - 1;
        this.processQueue();
    },

    /**
     * Convert ArrayBuffer to URL
     *
     * @param arrayBuffer   ArrayBuffer to convert
     * @param fileType      (optional) The file type (eg: "image/jpeg")
     * @return URL          URL string
     **/
    arrayBufferToURL: function (arrayBuffer, fileType) {
        var blob = this.createBlob(arrayBuffer);
        window.URL = window.URL || window.webkitURL;
        return window.URL.createObjectURL(blob);
    },

    /**
     * Create Blob from data
     *
     * @param part          ArrayBuffer, ArrayBufferView, Blob or DOMString part
     * @param fileType      (optional) The file type (eg: "image/jpeg")
     **/
    createBlob: function (part, fileType) {
        var blob;
        try {
            var properties = {};
            if (fileType) {
                properties.type = fileType;
            }
            blob = new Blob([part], properties);
        }
        catch (e) {
            // TypeError try old constructor
            window.BlobBuilder = window.BlobBuilder ||
                window.WebKitBlobBuilder;

            if (e.name == 'TypeError' && !window.BlobBuilder) {
                throw new Error('This platform does not support Blob type.');
            }

            var bb = new BlobBuilder();
            bb.append(part);
            blob = bb.getBlob(fileType);
        }
        return blob;
    }

};

